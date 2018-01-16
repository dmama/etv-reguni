package ch.vd.uniregctb.indexer.tiers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.avatar.AvatarService;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.BatchIterator;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ProgrammingException;
import ch.vd.uniregctb.common.StandardBatchIterator;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.common.Switchable;
import ch.vd.uniregctb.common.ThreadSwitch;
import ch.vd.uniregctb.indexer.GlobalIndexInterface;
import ch.vd.uniregctb.indexer.IndexableData;
import ch.vd.uniregctb.indexer.IndexerBatchException;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.async.MassTiersIndexer;
import ch.vd.uniregctb.indexer.async.OnTheFlyTiersIndexer;
import ch.vd.uniregctb.indexer.async.TiersIndexerWorker;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.load.BasicLoadMonitor;
import ch.vd.uniregctb.load.LoadAverager;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.stats.LoadMonitorable;
import ch.vd.uniregctb.stats.ServiceStats;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.IndividuNotFoundException;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersHelper;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.utils.LogLevel;

public class GlobalTiersIndexerImpl implements GlobalTiersIndexer, InitializingBean, DisposableBean {

	private static final int NANO_TO_MILLI = 1000000;

	private static final String ON_THE_FLY_SERVICE_NAME = "OnTheFlyIndexerQueueSize";

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalTiersIndexerImpl.class);

    private GlobalIndexInterface globalIndex;
    private GlobalTiersSearcher tiersSearcher;
    private TiersDAO tiersDAO;
    private PlatformTransactionManager transactionManager;
    private SessionFactory sessionFactory;
    private ServiceInfrastructureService serviceInfra;
    private ServiceCivilService serviceCivilService;
	private ServiceCivilCacheWarmer serviceCivilCacheWarmer;
	private ServiceOrganisationService serviceOrganisationService;
	private AssujettissementService assujettissementService;
	private AvatarService avatarService;
	private Dialect dialect;
	private StatsService statsService;
	private String serviceName;

	private OnTheFlyTiersIndexer onTheFlyTiersIndexer;
	private LoadAverager onTheFlyLoadAverager;
	private final ThreadSwitch onTheFlyIndexation = new ThreadSwitch(true);

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
	    public long startTimeOrganisation;
	    public long startTimeIndex;
        public long endTime;
        public long endTimeInfra;
	    public long endTimeCivil;
	    public long endTimeOrganisation;
	    public long endTimeIndex;
        public long indexerCpuTime;
        public long indexerExecTime;

	    public void start() {
		    startTime = System.nanoTime() / NANO_TO_MILLI;
		    startTimeInfra = getNanoInfra() / NANO_TO_MILLI;
		    startTimeCivil = getNanoCivil() / NANO_TO_MILLI;
		    startTimeOrganisation = getNanoOrganisation() / NANO_TO_MILLI;
		    startTimeIndex = getNanoIndex() / NANO_TO_MILLI;
	    }

	    public void end(MassTiersIndexer asyncIndexer) {
		    endTime = System.nanoTime() / NANO_TO_MILLI;
		    endTimeInfra = getNanoInfra() / NANO_TO_MILLI;
		    endTimeCivil = getNanoCivil() / NANO_TO_MILLI;
		    endTimeOrganisation = getNanoOrganisation() / NANO_TO_MILLI;
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
            long timeWaitOrganisation = endTimeOrganisation - startTimeOrganisation;
            long timeWaitIndex = endTimeIndex - startTimeIndex;
            long timeWaitAutres = timeWait - timeWaitInfra - timeWaitCivil - timeWaitOrganisation - timeWaitIndex;

            if (indexerExecTime == 0 || timeWait == 0) {
                LOGGER.debug("Statistiques d'indexation indisponibles !");
                return;
            }

            int percentCpu = (int) (100 * indexerCpuTime / indexerExecTime);
            int percentWait = 100 - percentCpu;
            int percentWaitInfra = (int) (100 * timeWaitInfra / timeWait);
            int percentWaitCivil = (int) (100 * timeWaitCivil / timeWait);
            int percentWaitOrganisation = (int) (100 * timeWaitOrganisation / timeWait);
            int percentWaitIndex = (int) (100 * timeWaitIndex / timeWait);
            int percentWaitAutres = 100 - percentWaitInfra - percentWaitCivil - percentWaitOrganisation - percentWaitIndex;

	        String log = "Temps total d'exécution         : " + timeTotal + " ms\n";
	        log += "Temps 'exec' threads indexation : " + indexerExecTime + " ms\n";
	        log += "Temps 'cpu' threads indexation  : " + indexerCpuTime + " ms" + " (" + percentCpu + "%)\n";
	        log += "Temps 'wait' threads indexation : " + timeWait + " ms" + " (" + percentWait + "%)\n";
	        log += " - service infrastructure       : " + (timeWaitInfra == 0 ? "<indisponible>\n" : timeWaitInfra + " ms" + " (" + percentWaitInfra + "%)\n");
	        log += " - service civil                : " + (timeWaitCivil == 0 ? "<indisponible>\n" : timeWaitCivil + " ms" + " (" + percentWaitCivil + "%)\n");
	        log += " - service organisation         : " + (timeWaitOrganisation == 0 ? "<indisponible>\n" : timeWaitOrganisation + " ms" + " (" + percentWaitOrganisation + "%)\n");
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

        private long getNanoOrganisation() {
            long timecivil = 0;
	        final ServiceStats stats = statsService.getServiceStats(ServiceOrganisationService.SERVICE_NAME);
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
	        final ServiceStats stats = statsService.getServiceStats(serviceName);
            if (stats != null) {
                timeindex = stats.getTotalTime();
            }
            return timeindex;
        }
    }

	@Override
	public int indexAllDatabase() throws IndexerException {
		return indexAllDatabase(null, 1, Mode.FULL);
	}

	@Override
	public int indexAllDatabase(@Nullable StatusManager statusManager, int nbThreads, Mode mode) throws
			IndexerException {

		if (statusManager == null) {
			statusManager = new LoggingStatusManager(LOGGER, LogLevel.Level.DEBUG);
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
			int nbIndexed = indexMultithreads(deltaIds.toAdd, nbThreads, mode, statusManager);
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
				{
					// JDE : on traite les identifiants dans l'ordre décroissant pour traiter les PP d'abord...
					final List<Long> allIds = new ArrayList<>(tiersDAO.getAllIds());
					allIds.sort(Collections.reverseOrder());
					deltaIds = new DeltaIds(allIds);
					break;
				}

				case DIRTY_ONLY:
					deltaIds = new DeltaIds(tiersDAO.getDirtyIds());
					break;

				case MISSING_ONLY:
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
			removeEntity(id);
			i++;
		}
	}

	private int indexMultithreads(List<Long> list, int nbThreads, Mode mode, StatusManager statusManager) throws Exception {

		LOGGER.info("ASYNC indexation de " + list.size() + " tiers par " + nbThreads + " threads en mode " + mode);

		final TimeLog timeLog = new TimeLog();
		timeLog.start();

		final int queueSizeByThread = TiersIndexerWorker.BATCH_SIZE;
		final MassTiersIndexer asyncIndexer = createMassTiersIndexer(nbThreads, mode, queueSizeByThread);

		final int size = list.size();

		// variables pour le log
		int i = 0;

		// période de la boucle d'attente lors du remplissage complet de la queue de traitement
		final Duration offerTimeout = getOfferTimeout();

		// sera mis à "true" si on détecte que tous les threads sont morts prématurément
		boolean deadThreads = false;

		final BatchIterator<Long> iter = new StandardBatchIterator<>(list, 100);
		while (iter.hasNext() && !statusManager.isInterrupted() && !deadThreads) {

			final Set<Long> ids = new HashSet<>(iter.next());

			statusManager.setMessage("Indexation du tiers " + i + " sur " + size, (100 * i) / size);

			// Dispatching des tiers à indexer
			for (Long id : ids) {
				if (statusManager.isInterrupted()) {
					asyncIndexer.clearQueue();
					break;
				}

				// insère l'id dans la queue à indexer, mais de manière à pouvoir interrompre le processus si
				// plus personne ne prélève de tiers dans la queue (p.a. si tous les threads d'indexations sont morts).
				while (!asyncIndexer.offerTiersForIndexation(id, offerTimeout) && !statusManager.isInterrupted()) {

					// si tous les threads sont morts, il est temps de tout arrêter...
					if (!asyncIndexer.isAlive()) {
						LOGGER.debug("Détecté que tous les threads d'indexation sont morts avant la demande d'arrêt.");
						deadThreads = true;
						break;
					}

					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug(String.format("La queue d'indexation est pleine, attente de %d milli-secondes...", offerTimeout.toMillis()));
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
		else if (statusManager.isInterrupted()) {
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
	 * @param nbThreads                 le nombre de threads pour l'indexation en parallèle
	 * @param mode                      le mode d'indexation
	 * @param queueSizeByThread         la taille maximale de la queue par thread
	 * @return l'indexer de la classe {@link MassTiersIndexer}
	 */
	protected MassTiersIndexer createMassTiersIndexer(int nbThreads, Mode mode, int queueSizeByThread) {
		return new MassTiersIndexer(this, transactionManager, sessionFactory, nbThreads, queueSizeByThread, mode, dialect, serviceCivilCacheWarmer);
	}

	/**
	 * Surchargeable dans les tests pour avoir des temps de latence plus faibles
	 * @return le délai d'attente, quand la queue est pleine
	 */
	protected Duration getOfferTimeout() {
		return Duration.ofSeconds(10);
	}

	private static class DeltaIds {
		public final List<Long> toAdd;
		public final List<Long> toRemove ;

		private DeltaIds() {
			this.toAdd = new ArrayList<>();
			this.toRemove = new ArrayList<>();
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

		final Set<Long> idsDb = new HashSet<>(tiersDAO.getAllIds());
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

		final List<IndexableData> data = new ArrayList<>();
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
        final List<TiersIndexable> indexables = new ArrayList<>();
        if (followDependents) {
            final Collection<Tiers> list = buildDependents(tiers);
            for (Tiers t : list) {
                indexables.add(buildIndexable(t));
            }
        }
        indexables.add(buildIndexable(tiers));
        return indexables;
    }

	/**
	 * @param tiers un tiers de départ
	 * @return une collection des tiers dépendants du tiers de départ (celui-ci non-compris)
	 */
    private Collection<Tiers> buildDependents(Tiers tiers) {
	    final Map<Long, Tiers> map = new HashMap<>();

	    // on commence par ajouter le tiers de départ pour gérer les boucles de récursion
	    map.put(tiers.getNumero(), tiers);

	    // calcul récursif de dépendance
	    fillDependentMap(tiers, map);

	    // le tiers lui-même ne doit pas faire partie de la liste des dépendants...
	    map.remove(tiers.getNumero());

	    return map.values();
    }

	private void fillDependentMap(Tiers tiers, @NotNull Map<Long, Tiers> map) {

		// une map locale pour les départs de récursion
		final Map<Long, Tiers> mapNew = new HashMap<>();

        // Personne physique
        if (tiers instanceof PersonnePhysique) {
            final List<RapportEntreTiers> rapports = TiersHelper.getRapportSujetHistoOfType(tiers, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
            if (rapports != null && !rapports.isEmpty()) {
                for (RapportEntreTiers r : rapports) {
	                if (!map.containsKey(r.getObjetId())) {
		                final MenageCommun menage = (MenageCommun) tiersDAO.get(r.getObjetId());
		                map.put(r.getObjetId(), menage);
		                mapNew.put(r.getObjetId(), menage);
	                }
                }
            }
        }

        // MenageCommun
        else if (tiers instanceof MenageCommun) {
            final List<RapportEntreTiers> rapports = TiersHelper.getRapportObjetHistoOfType(tiers, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
            if (rapports != null && !rapports.isEmpty()) {
                for (RapportEntreTiers r : rapports) {
	                if (!map.containsKey(r.getSujetId())) {
		                final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(r.getSujetId());
		                map.put(r.getSujetId(), pp);
		                mapNew.put(r.getSujetId(), pp);
	                }
                }
            }
        }

        // Reindex les DPI si on a un CTB
        if (tiers instanceof Contribuable) {
            final Contribuable ctb = (Contribuable) tiers;
            final Set<DebiteurPrestationImposable> dpis = tiersService.getDebiteursPrestationImposable(ctb);
            if (dpis != null) {
                for (DebiteurPrestationImposable dpi : dpis) {
	                if (!map.containsKey(dpi.getNumero())) {
		                map.put(dpi.getNumero(), dpi);
		                mapNew.put(dpi.getNumero(), dpi);
	                }
                }
            }
        }

	    // [SIFISC-19632] Ré-indexe les établissements si on a une entreprise
	    if (tiers instanceof Entreprise) {
		    final List<RapportEntreTiers> rapports = TiersHelper.getRapportSujetHistoOfType(tiers, TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE);
		    if (rapports != null && !rapports.isEmpty()) {
			    for (RapportEntreTiers ret : rapports) {
				    if (!map.containsKey(ret.getObjetId())) {
					    final Etablissement etb = (Etablissement) tiersDAO.get(ret.getObjetId());
					    map.put(ret.getObjetId(), etb);
					    mapNew.put(ret.getObjetId(), etb);
				    }
			    }
		    }
	    }

		// un peu de récursivité sur les nouveaux ajoutés
		// (premier cas utile de cette récursivité : DPI sur établissement, alors que c'est l'entreprise qui doit être indexée, à la base)
		// (mais on pouvait déjà avoir le cas avant du DPI sur un ménage...)
		for (Tiers nouveau : mapNew.values()) {
			fillDependentMap(nouveau, map);
		}
    }

    private TiersIndexable buildIndexable(Tiers tiers) {

        final TiersIndexable indexable;

        if (tiers instanceof DebiteurPrestationImposable) {
            final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;
            indexable = new DebiteurPrestationImposableIndexable(adresseService, tiersService, assujettissementService, serviceCivilService, serviceOrganisationService, serviceInfra, avatarService, dpi);
        }
        else if (tiers instanceof PersonnePhysique) {
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
                indexable = new HabitantIndexable(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, pp, individu);
            }
            // NonHabitant
            else {
                indexable = new NonHabitantIndexable(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, pp);
            }
        }
        else if (tiers instanceof MenageCommun) {
            final MenageCommun cmc = (MenageCommun) tiers;
            indexable = new MenageCommunIndexable(adresseService, tiersService, assujettissementService, serviceCivilService, serviceInfra, avatarService, cmc);
        }
        else if (tiers instanceof Entreprise) {
            final Entreprise entreprise = (Entreprise) tiers;
	        indexable = new EntrepriseIndexable(adresseService, tiersService, assujettissementService, serviceInfra, serviceOrganisationService, avatarService, entreprise);
        }
        else if (tiers instanceof AutreCommunaute) {
            final AutreCommunaute autreCommunaute = (AutreCommunaute) tiers;
            indexable = new AutreCommunauteIndexable(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, autreCommunaute);
        }
        else if (tiers instanceof CollectiviteAdministrative) {
            final CollectiviteAdministrative collectivite = (CollectiviteAdministrative) tiers;
            indexable = new CollectiviteAdministrativeIndexable(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, collectivite);
        }
        else if (tiers instanceof Etablissement) {
	        final Etablissement etablissement = (Etablissement) tiers;
	        indexable = new EtablissementIndexable(adresseService, tiersService, assujettissementService, serviceInfra, serviceOrganisationService, avatarService, etablissement);
        }
        else {
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

    public void removeEntity(Long id) {
        globalIndex.removeEntity(id);
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
		if (statsService != null) {

			// façade de monitoring sur la queue d'indexation on-the-fly, où la charge est définie comme le nombre de tiers en attente d'indexation
			final LoadMonitorable service = this::getOnTheFlyQueueSize;

			// calculateur de moyenne de charge sur les 5 dernières minutes (échantillonnage à 2 fois par seconde)
			onTheFlyLoadAverager = new LoadAverager(service, ON_THE_FLY_SERVICE_NAME, 600, 500);
			onTheFlyLoadAverager.start();

			// enregistrement du monitoring
			statsService.registerLoadMonitor(ON_THE_FLY_SERVICE_NAME, new BasicLoadMonitor(service, onTheFlyLoadAverager));
		}
	}

	@Override
	public void destroy() throws Exception {
		onTheFlyTiersIndexer.destroy();
		if (onTheFlyLoadAverager != null) {
			onTheFlyLoadAverager.stop();
			onTheFlyLoadAverager = null;
		}
		if (statsService != null) {
			statsService.unregisterLoadMonitor(ON_THE_FLY_SERVICE_NAME);
		}
	}

	@Override
	public Switchable onTheFlyIndexationSwitch() {
		return this.onTheFlyIndexation;
	}

	@Override
	public int getOnTheFlyQueueSize() {
		return onTheFlyTiersIndexer.getQueueSize();
	}

	@Override
	public int getOnTheFlyThreadNumber() {
		return onTheFlyTiersIndexer.getActiveThreadNumber();
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

	public void setServiceCivilCacheWarmer(ServiceCivilCacheWarmer serviceCivilCacheWarmer) {
		this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
	}

	@SuppressWarnings({"UnusedDeclaration"})
    public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
        this.serviceInfra = serviceInfra;
    }

	public void setTiersService(TiersService tiersService) {
        this.tiersService = tiersService;
    }

	public void setAssujettissementService(AssujettissementService assujettissementService) {
		this.assujettissementService = assujettissementService;
	}

	public void setAvatarService(AvatarService avatarService) {
		this.avatarService = avatarService;
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

	public void setServiceOrganisationService(ServiceOrganisationService serviceOrganisationService) {
		this.serviceOrganisationService = serviceOrganisationService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDialect(Dialect dialect) {
		this.dialect = dialect;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
}
