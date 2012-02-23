package ch.vd.uniregctb.indexer.tiers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.BatchIterator;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ProgrammingException;
import ch.vd.uniregctb.common.StandardBatchIterator;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.indexer.GlobalIndexInterface;
import ch.vd.uniregctb.indexer.GlobalIndexTracing;
import ch.vd.uniregctb.indexer.IndexableData;
import ch.vd.uniregctb.indexer.IndexerBatchException;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.async.MassTiersIndexer;
import ch.vd.uniregctb.indexer.async.OnTheFlyTiersIndexer;
import ch.vd.uniregctb.indexer.async.TiersIndexerWorker;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;
import ch.vd.uniregctb.stats.ServiceStats;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.IndividuNotFoundException;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersHelper;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class GlobalTiersIndexerImpl implements GlobalTiersIndexer, InitializingBean, DisposableBean {

	private static final int NANO_TO_MILLI = 1000000;

    private static final Logger LOGGER = Logger.getLogger(GlobalTiersIndexerImpl.class);

    private GlobalIndexInterface globalIndex;
    private GlobalTiersSearcher tiersSearcher;
    private TiersDAO tiersDAO;
    private PlatformTransactionManager transactionManager;
    private SessionFactory sessionFactory;
    private ServiceInfrastructureService serviceInfra;
    private ServiceCivilService serviceCivilService;
	private ServicePersonneMoraleService servicePM;
	private Dialect dialect;
	private StatsService statsService;

	private OnTheFlyTiersIndexer onTheFlyTiersIndexer;

	private static class Behavior {
        public boolean onTheFlyIndexation = true;
    }

    private final ThreadLocal<Behavior> byThreadBehavior = new ThreadLocal<Behavior>();

    /**
     * Le service qui fournit les adresses et autres
     */
    private AdresseService adresseService;

    private TiersService tiersService;

    @Override
    public void overwriteIndex() {
	    onTheFlyTiersIndexer.reset();
        globalIndex.overwriteIndex();
    }

    private class TimeLog {
        public long startTime;
        public long startTimeInfra;
	    public long startTimeCivil;
	    public long startTimePM;
	    public long startTimeIndex;
        public long endTime;
        public long endTimeInfra;
	    public long endTimeCivil;
	    public long endTimePM;
	    public long endTimeIndex;
        public long indexerCpuTime;
        public long indexerExecTime;

	    public void start() {
		    startTime = System.nanoTime() / NANO_TO_MILLI;
		    startTimeInfra = getNanoInfra() / NANO_TO_MILLI;
		    startTimeCivil = getNanoCivil() / NANO_TO_MILLI;
		    startTimePM = getNanoPM() / NANO_TO_MILLI;
		    startTimeIndex = getNanoIndex() / NANO_TO_MILLI;
	    }

	    public void end(MassTiersIndexer asyncIndexer) {
		    endTime = System.nanoTime() / NANO_TO_MILLI;
		    endTimeInfra = getNanoInfra() / NANO_TO_MILLI;
		    endTimeCivil = getNanoCivil() / NANO_TO_MILLI;
		    endTimePM = getNanoPM() / NANO_TO_MILLI;
		    endTimeIndex = getNanoIndex() / NANO_TO_MILLI;
		    indexerCpuTime = asyncIndexer.getTotalCpuTime() / NANO_TO_MILLI;
		    indexerExecTime = asyncIndexer.getTotalExecTime() / NANO_TO_MILLI;
	    }

	    public void logStats() {

            // détermine les différents statistiques de temps en millisecondes
            long timeTotal = endTime - startTime;
            long timeWait = indexerExecTime - indexerCpuTime;
            long timeWaitInfra = endTimeInfra - startTimeInfra;
            long timeWaitCivil = endTimeCivil - startTimeCivil;
            long timeWaitPM = endTimePM - startTimePM;
            long timeWaitIndex = endTimeIndex - startTimeIndex;
            long timeWaitAutres = timeWait - timeWaitInfra - timeWaitCivil - timeWaitIndex;

            if (indexerExecTime == 0 || timeWait == 0) {
                LOGGER.debug("Statistiques d'indexation indisponibles !");
                return;
            }

            int percentCpu = (int) (100 * indexerCpuTime / indexerExecTime);
            int percentWait = 100 - percentCpu;
            int percentWaitInfra = (int) (100 * timeWaitInfra / timeWait);
            int percentWaitCivil = (int) (100 * timeWaitCivil / timeWait);
            int percentWaitPM = (int) (100 * timeWaitPM / timeWait);
            int percentWaitIndex = (int) (100 * timeWaitIndex / timeWait);
            int percentWaitAutres = 100 - percentWaitInfra - percentWaitCivil - percentWaitIndex;

	        String log = "Temps total d'exécution         : " + timeTotal + " ms\n";
	        log += "Temps 'exec' threads indexation : " + indexerExecTime + " ms\n";
	        log += "Temps 'cpu' threads indexation  : " + indexerCpuTime + " ms" + " (" + percentCpu + "%)\n";
	        log += "Temps 'wait' threads indexation : " + timeWait + " ms" + " (" + percentWait + "%)\n";
	        log += " - service infrastructure       : " + (timeWaitInfra == 0 ? "<indisponible>\n" : timeWaitInfra + " ms" + " (" + percentWaitInfra + "%)\n");
	        log += " - service civil                : " + (timeWaitCivil == 0 ? "<indisponible>\n" : timeWaitCivil + " ms" + " (" + percentWaitCivil + "%)\n");
	        log += " - service pm                   : " + (timeWaitPM == 0 ? "<indisponible>\n" : timeWaitPM + " ms" + " (" + percentWaitPM + "%)\n");
	        log += " - indexer                      : " + (timeWaitIndex == 0 ? "<indisponible>\n" : timeWaitIndex + " ms" + " (" + percentWaitIndex + "%)\n");
	        log += " - autre (scheduler, jdbc, ...) : " + timeWaitAutres + " ms" + " (" + percentWaitAutres + "%)";
	        LOGGER.info(log);
        }

        private long getNanoCivil() {
            long timecivil = 0;
	        final ServiceStats stats = statsService.getServiceStats(ServiceCivilService.SERVICE_NAME);
            if (stats != null) {
                timecivil = stats.getTotalTime();
            }
            return timecivil;
        }

        private long getNanoPM() {
            long timecivil = 0;
	        final ServiceStats stats = statsService.getServiceStats(ServicePersonneMoraleService.SERVICE_NAME);
            if (stats != null) {
                timecivil = stats.getTotalTime();
            }
            return timecivil;
        }

        private long getNanoInfra() {
            long timeinfra = 0;
	        final ServiceStats stats = statsService.getServiceStats(ServiceInfrastructureService.SERVICE_NAME);
            if (stats != null) {
                timeinfra = stats.getTotalTime();
            }
            return timeinfra;
        }

        private long getNanoIndex() {
            long timeindex = 0;
	        final ServiceStats stats = statsService.getServiceStats(GlobalIndexTracing.SERVICE_NAME);
            if (stats != null) {
                timeindex = stats.getTotalTime();
            }
            return timeindex;
        }
    }

	@Override
	public int indexAllDatabase() throws IndexerException {
		return indexAllDatabase(null, 1, Mode.FULL, true);
	}

    @Override
    public int indexAllDatabase(@Nullable StatusManager statusManager, int nbThreads, Mode mode, boolean prefetchIndividus)
            throws IndexerException {

        if (statusManager == null) {
            statusManager = new LoggingStatusManager(LOGGER, Level.DEBUG);
        }

        Audit.info("Réindexation de la base de données (mode = " + mode + ')');

        if (mode == Mode.FULL) {
            // Ecrase l'indexe lucene sur le disque local
            statusManager.setMessage("Efface le repertoire d'indexation");
            overwriteIndex();
        }

        statusManager.setMessage("Récupération des tiers à indexer...");
	    final DeltaIds deltaIds = getIdsToIndex(mode);

	    try {
		    int nbIndexed = indexMultithreads(deltaIds.toAdd, nbThreads, mode, prefetchIndividus, statusManager);
		    remove(deltaIds.toRemove, statusManager);

		    // [SIFISC-1184] on détecte et supprime les doublons une fois l'indexation effectuée
			statusManager.setMessage("Suppression des doublons...");
			globalIndex.deleteDuplicate();

		    return nbIndexed;
	    }
	    catch (Exception e) {
		    Audit.error("Erreur lors de l'indexation: " + e.getMessage());
		    throw new IndexerException(e);
	    }
    }

	private DeltaIds getIdsToIndex(final Mode mode) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		return template.execute(new TransactionCallback<DeltaIds>() {
			@Override
			public DeltaIds doInTransaction(TransactionStatus status) {
				
				final DeltaIds deltaIds;
				switch (mode) {
				case FULL:
					deltaIds = new DeltaIds(tiersDAO.getAllIds());
					break;

				case DIRTY_ONLY:
					deltaIds = new DeltaIds(tiersDAO.getDirtyIds());
					break;

				case INCREMENTAL:
					deltaIds = getIncrementalIds();
					break;

				default:
					throw new ProgrammingException("Mode d'indexation inconnu = " + mode);
				}
				return deltaIds;
			}
		});
	}

	/**
	 * [UNIREG-1988] Supprime les tiers spécifiés de l'indexeur.
	 *
	 * @param ids           les ids des tiers à supprimer
	 * @param statusManager un status manager
	 */
	private void remove(List<Long> ids, StatusManager statusManager) {

		LOGGER.info("Suppression de l'indexeur de " + ids.size() + " tiers");

		final int size = ids.size();
		int i = 0;
		
		for (Long id : ids) {
			statusManager.setMessage("Suppression du tiers " + id, (100 * i) / size);
			removeEntity(id, TiersIndexable.TYPE);
			i++;
		}
	}

	private int indexMultithreads(List<Long> list, int nbThreads, Mode mode, boolean prefetchIndividus, StatusManager statusManager) throws Exception {

		LOGGER.info("ASYNC indexation de " + list.size() + " tiers par " + nbThreads + " threads en mode " + mode
				+ (prefetchIndividus ? " avec" : " sans") + " préchargement des individus");

		final TimeLog timeLog = new TimeLog();
		timeLog.start();

		final int queueSizeByThread = TiersIndexerWorker.BATCH_SIZE;
		final MassTiersIndexer asyncIndexer = createMassTiersIndexer(nbThreads, mode, queueSizeByThread, prefetchIndividus);

		final int size = list.size();

		// variables pour le log
		int i = 0;

		// période de la boucle d'attente lors du remplissage complet de la queue de traitement
		final int offerTimeout = getOfferTimeoutInSeconds();

		// sera mis à "true" si on détecte que tous les threads sont morts prématurément
		boolean deadThreads = false;

		final BatchIterator<Long> iter = new StandardBatchIterator<Long>(list, 100);
		while (iter.hasNext() && !statusManager.interrupted() && !deadThreads) {

			final Set<Long> ids = new HashSet<Long>(iter.next());

			statusManager.setMessage("Indexation du tiers " + i + " sur " + size, (100 * i) / size);

			// Dispatching des tiers à indexer
			for (Long id : ids) {
				if (statusManager.interrupted()) {
					asyncIndexer.clearQueue();
					break;
				}

				// insère l'id dans la queue à indexer, mais de manière à pouvoir interrompre le processus si
				// plus personne ne prélève de tiers dans la queue (p.a. si tous les threads d'indexations sont morts).
				while (!asyncIndexer.offerTiersForIndexation(id, offerTimeout, TimeUnit.SECONDS) && !statusManager.interrupted()) {

					// si tous les threads sont morts, il est temps de tout arrêter...
					if (!asyncIndexer.isAlive()) {
						LOGGER.debug("Détecté que tous les threads d'indexation sont morts avant la demande d'arrêt.");
						deadThreads = true;
						break;
					}

					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug(String.format("La queue d'indexation est pleine, attente de %d secondes...", offerTimeout));
					}
				}

				if (deadThreads) {
					break;
				}

				++i;
			}
		}

		deadThreads = asyncIndexer.terminate() || deadThreads;

		timeLog.end(asyncIndexer);
		timeLog.logStats();

		if (deadThreads) {
			final String msg = String.format("Les threads d'indexation se sont tous arrêtés. Nombre de tiers réindexés/total = %d/%d", i, size);
			Audit.error(msg);
			throw new IndexerException(msg);
		}
		else if (statusManager.interrupted()) {
			Audit.warn("L'indexation a été interrompue. Nombre de tiers réindexés/total = " + i + '/' + size);
		}
		else {
			Audit.success("L'indexation s'est terminée avec succès. Nombre de tiers réindexés = " + size);
		}

		return size;
	}

	/**
	 * Surchargeable dans les tests pour provoquer des situations spéciales
	 *
	 * @param nbThreads         le nombre de threads pour l'indexation en parallèle
	 * @param mode              le mode d'indexation
	 * @param queueSizeByThread la taille maximale de la queue par thread
	 * @param prefetchIndividus <b>vrai</b> si le cache des individus doit être préchauffé par lot; <b>faux</b> autrement.
	 * @return l'indexer de la classe {@link MassTiersIndexer}
	 */
	protected MassTiersIndexer createMassTiersIndexer(int nbThreads, Mode mode, int queueSizeByThread, boolean prefetchIndividus) {
		return new MassTiersIndexer(this, transactionManager, sessionFactory, nbThreads, queueSizeByThread, mode, dialect, prefetchIndividus, serviceCivilService);
	}

	/**
	 * Surchargeable dans les tests pour avoir des temps de latence plus faibles
	 * @return la délai d'attente, en secondes, quand la queue est pleine
	 */
	protected int getOfferTimeoutInSeconds() {
		return 10;
	}

	private static class DeltaIds {
		public final List<Long> toAdd;
		public final List<Long> toRemove ;

		private DeltaIds() {
			this.toAdd = new ArrayList<Long>();
			this.toRemove = new ArrayList<Long>();
		}

		private DeltaIds(List<Long> toAdd) {
			this.toAdd = toAdd;
			this.toRemove = Collections.emptyList();
		}
	}

	/**
	 * @return la liste des ids non-indexés, ainsi que ceux indexés à tord
	 */
	private DeltaIds getIncrementalIds() {

		final Set<Long> idsDb = new HashSet<Long>(tiersDAO.getAllIds());
		final Set<Long> idsIndex = tiersSearcher.getAllIds();

		DeltaIds ids = new DeltaIds();

		for (Long id : idsDb) {
			if (!idsIndex.contains(id)) {
				ids.toAdd.add(id);
			}
		}
		for (Long id : idsIndex) {
			if (!idsDb.contains(id)) {
				ids.toRemove.add(id);
			}
		}

		return ids;
	}

	/**
	 * Index les tiers spécifié.
	 *
	 * @param tiers            les tiers à indexer
	 * @param removeBefore     si <b>vrai</b> les données du tiers seront supprimée de l'index avant d'être réinsérée; si <b>false</b> les données seront simplement ajoutées.
	 * @param followDependents si <b>vrai</b> les tiers liés (ménage commun, ...) seront aussi indexées.
	 * @throws IndexerBatchException en cas d'exception lors de l'indexation d'un ou plusieurs tiers. La méthode essaie d'indexer tous les tiers dans tous les cas, ce qui veut dire que si le premier
	 *                               tiers lève une exception, les tiers suivants seront quand même indexés.
	 */
    public void indexTiers(List<Tiers> tiers, boolean removeBefore, boolean followDependents) throws IndexerBatchException {
        Assert.notNull(tiers);

        // Note : en cas d'exception, on continue le processing, on stocke les exceptions et on les lèves d'un seul coup à la fin
        IndexerBatchException exception = null;

        // construit la liste des indexables

		final List<IndexableData> data = new ArrayList<IndexableData>();
        for (Tiers t : tiers) {
            try {
                final List<TiersIndexable> indexables = buildIndexables(t, followDependents);
	            for (TiersIndexable i : indexables) {
	                data.add(i.getIndexableData()); // [UNIREG-2390] on construit les datas ici, de manière à catcher l'exception pour le tiers et pas pour le batch complet (comme c'était fait avant)
	            }
            }
            catch (Exception e) {
                if (exception == null) {
                    exception = new IndexerBatchException();
                }
                exception.addException(t, e);
            }
        }

        try {
	        // process les indexables
	        indexBatch(data, removeBefore);
        }
        catch (IndexerBatchException e) {
	        // un ou plusieurs tiers n'ont pas pu être réindexés
            if (exception == null) {
                exception = e;
            }
	        else {
	            exception.add(e);
            }
        }
        catch (Exception e) {
	        // potentiellement aucun des tiers n'a pu être réindexé
            if (exception == null) {
                exception = new IndexerBatchException();
            }
	        for (Tiers t : tiers) {
		        exception.addException(t, e);
	        }
        }

        if (exception != null) {
            throw exception;
        }
    }


    private List<TiersIndexable> buildIndexables(Tiers tiers, boolean followDependents) {

        final List<TiersIndexable> indexables = new ArrayList<TiersIndexable>();

        if (followDependents) {
            List<Tiers> list = buildDependents(tiers);
            for (Tiers t : list) {
                indexables.add(buildIndexable(t));
            }
        }
        indexables.add(buildIndexable(tiers));

        return indexables;
    }

    private List<Tiers> buildDependents(Tiers tiers) {

        List<Tiers> list = new ArrayList<Tiers>();

        // Debiteur prestation imposable
        if (tiers instanceof PersonnePhysique) {
            // Habitant
            List<RapportEntreTiers> rapports = TiersHelper.getRapportSujetHistoOfType(tiers, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
            if (rapports != null && !rapports.isEmpty()) {
                for (RapportEntreTiers r : rapports) {
                    MenageCommun menage = (MenageCommun) tiersDAO.get(r.getObjetId());
                    list.add(menage);
                }
            }
        }

        // MenageCommun
        else if (tiers instanceof MenageCommun) {
            List<RapportEntreTiers> rapports = TiersHelper.getRapportObjetHistoOfType(tiers, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
            if (rapports != null && !rapports.isEmpty()) {
                for (RapportEntreTiers r : rapports) {
                    PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(r.getSujetId());
                    list.add(pp);
                }
            }
        }

        // Reindex les DPI si on a un CTB
        if (tiers instanceof Contribuable) {
            Contribuable ctb = (Contribuable) tiers;
            Set<DebiteurPrestationImposable> dpis = tiersService.getDebiteursPrestationImposable(ctb);
            if (dpis != null) {
                for (DebiteurPrestationImposable dpi : dpis) {
                    list.add(dpi);
                }
            }
        }
        return list;
    }

    private TiersIndexable buildIndexable(Tiers tiers) {

        final TiersIndexable indexable;

        if (tiers instanceof DebiteurPrestationImposable) {
            final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;
            indexable = new DebiteurPrestationImposableIndexable(adresseService, tiersService, serviceCivilService, servicePM, serviceInfra, dpi);
        } else if (tiers instanceof PersonnePhysique) {
            final PersonnePhysique pp = (PersonnePhysique) tiers;
            // Habitant
            if (pp.isHabitantVD()) {
                final Long numeroIndividu = pp.getNumeroIndividu();
                Assert.notNull(numeroIndividu);
                // Recuperation de l'individu
                final Individu individu = serviceCivilService.getIndividu(numeroIndividu, null, AttributeIndividu.ADRESSES);
	            if (individu == null) {
		            throw new IndividuNotFoundException(pp);
	            }
                indexable = new HabitantIndexable(adresseService, tiersService, serviceInfra, pp, individu);
            }
            // NonHabitant
            else {
                indexable = new NonHabitantIndexable(adresseService, tiersService, serviceInfra, pp);
            }
        } else if (tiers instanceof MenageCommun) {
            final MenageCommun cmc = (MenageCommun) tiers;
            indexable = new MenageCommunIndexable(adresseService, tiersService, serviceCivilService, serviceInfra, cmc);
        } else if (tiers instanceof Entreprise) {
            final Entreprise entreprise = (Entreprise) tiers;
	        indexable = new EntrepriseIndexable(adresseService, tiersService, serviceInfra, servicePM, entreprise);
        } else if (tiers instanceof AutreCommunaute) {
            final AutreCommunaute autreCommunaute = (AutreCommunaute) tiers;
            indexable = new AutreCommunauteIndexable(adresseService, tiersService, serviceInfra, autreCommunaute);
        } else if (tiers instanceof CollectiviteAdministrative) {
            final CollectiviteAdministrative collectivite = (CollectiviteAdministrative) tiers;
            indexable = new CollectiviteAdministrativeIndexable(adresseService, tiersService, serviceInfra, collectivite);
        } else {
            final String message = "Le Tiers " + tiers.getNatureTiers() + " n'est pas connu de l'indexation!!!";
            LOGGER.error(message);
            throw new IndexerException(tiers, message);
        }

        return indexable;
    }

    private void indexBatch(List<IndexableData> data, boolean removeBefore) throws IndexerBatchException {

	    // Note : en cas d'exception, on continue le processing, on stocke les exceptions et on les lèves d'un seul coup à la fin
	    IndexerBatchException exception = null;

	    try {
		    // on essaie d'indexer tous les données en vrac
		    if (removeBefore) {
			    globalIndex.removeThenIndexEntities(data);
		    }
		    else {
			    globalIndex.indexEntities(data);
		    }
	    }
	    catch (Exception e) {
		    // [UNIREG-2390] en cas d'erreur, on reprend les datas un à un de manière à lancer une exception aussi précise que possible
		    for (IndexableData d : data) {
			    try {
					globalIndex.removeThenIndexEntity(d);
			    }
			    catch (Exception ee) {
				    if (exception == null) {
				        exception = new IndexerBatchException();
				    }
				    exception.addException(d.getId(), e);
			    }
		    }
	    }

	    if (exception != null) {
	        throw exception;
	    }
    }

    public void removeEntity(Long id, String type) {
        globalIndex.removeEntity(id, type);
    }

	@Override
	public void schedule(long id) {
		onTheFlyTiersIndexer.schedule(id);
	}

	@Override
	public void schedule(Collection<Long> ids) {
		onTheFlyTiersIndexer.schedule(ids);
	}

	@Override
	public void sync() {
		onTheFlyTiersIndexer.sync();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		onTheFlyTiersIndexer = new OnTheFlyTiersIndexer(this, transactionManager, sessionFactory, dialect);
	}

	@Override
	public void destroy() throws Exception {
		onTheFlyTiersIndexer.destroy();
	}

    private Behavior getByThreadBehavior() {
        Behavior behavior = this.byThreadBehavior.get();
        if (behavior == null) {
            behavior = new Behavior();
            this.byThreadBehavior.set(behavior);
        }
        return behavior;
    }

    @Override
    public boolean isOnTheFlyIndexation() {
        return getByThreadBehavior().onTheFlyIndexation;
    }

    @Override
    public void setOnTheFlyIndexation(boolean onTheFlyIndexation) {
        getByThreadBehavior().onTheFlyIndexation = onTheFlyIndexation;
    }

	@SuppressWarnings({"UnusedDeclaration"})
    public void setGlobalIndex(GlobalIndexInterface globalIndex) {
        this.globalIndex = globalIndex;
    }

    public void setTiersDAO(TiersDAO tiersDAO) {
        this.tiersDAO = tiersDAO;
    }

    public void setServiceCivilService(ServiceCivilService serviceCivilService) {
        this.serviceCivilService = serviceCivilService;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
        this.serviceInfra = serviceInfra;
    }

    public void setTiersService(TiersService tiersService) {
        this.tiersService = tiersService;
    }

    public void setAdresseService(AdresseService adresseService) {
        this.adresseService = adresseService;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setTiersSearcher(GlobalTiersSearcher tiersSearcher) {
        this.tiersSearcher = tiersSearcher;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServicePM(ServicePersonneMoraleService servicePM) {
		this.servicePM = servicePM;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDialect(Dialect dialect) {
		this.dialect = dialect;
	}
}
