package ch.vd.unireg.indexer.tiers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.simpleindexer.LuceneException;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.avatar.AvatarService;
import ch.vd.unireg.cache.ServiceCivilCacheWarmer;
import ch.vd.unireg.common.BatchIterator;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.ProgrammingException;
import ch.vd.unireg.common.StandardBatchIterator;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.Switchable;
import ch.vd.unireg.common.ThreadSwitch;
import ch.vd.unireg.indexer.GlobalIndexInterface;
import ch.vd.unireg.indexer.IndexableData;
import ch.vd.unireg.indexer.IndexerBatchException;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.indexer.async.MassTiersIndexer;
import ch.vd.unireg.indexer.async.OnTheFlyTiersIndexer;
import ch.vd.unireg.indexer.async.TiersIndexerWorker;
import ch.vd.unireg.indexer.lucene.LuceneHelper;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.interfaces.service.ServiceOrganisationService;
import ch.vd.unireg.load.BasicLoadMonitor;
import ch.vd.unireg.load.LoadAverager;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.stats.LoadMonitorable;
import ch.vd.unireg.stats.ServiceStats;
import ch.vd.unireg.stats.StatsService;
import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.IndividuNotFoundException;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersHelper;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.TypeTiers;
import ch.vd.unireg.type.TypeRapportEntreTiers;
import ch.vd.unireg.utils.LogLevel;

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
		return indexAllDatabase(Mode.FULL, 1, null);
	}

	@Override
	public int indexAllDatabase(@NotNull Mode mode, int nbThreads, @Nullable StatusManager statusManager) throws IndexerException {
    	// on prends en compte toute la population
		return indexAllDatabase(mode, EnumSet.allOf(TypeTiers.class), nbThreads, statusManager);
	}

	@Override
	public int indexAllDatabase(@NotNull GlobalTiersIndexer.Mode mode, @NotNull Set<TypeTiers> typesTiers, int nbThreads, @Nullable StatusManager statusManager) throws IndexerException {

		if (statusManager == null) {
			statusManager = new LoggingStatusManager(LOGGER, LogLevel.Level.DEBUG);
		}

		Audit.info("Réindexation de la base de données (mode = " + mode + ", typesTiers = " + typesTiers + ")");
		final Date indexationStart = DateHelper.getCurrentDate();

		if (mode == Mode.FULL) {
			// Ecrase l'indexe lucene sur le disque local
			statusManager.setMessage("Effacement du répertoire d'indexation...");
			overwriteIndex();
		}

		statusManager.setMessage("Récupération des tiers à indexer...");
		final DeltaIds deltaIds = getIdsToIndex(mode, typesTiers);

		try {
			int nbIndexed = indexMultithreads(deltaIds.toAdd, nbThreads, mode, statusManager);
			remove(deltaIds.toRemove, statusManager);

			if (mode == Mode.FULL_INCREMENTAL) {
				// on supprime les tiers non-indexés dans la phase incrémentale (car cela veut dire qu'ils n'existent plus)
				statusManager.setMessage("Nettoyage des tiers surnuméraires...");
				deleteTiersIndexedBefore(indexationStart, typesTiers);
			}

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

	/**
	 * Supprime de l'indexe les éléments dont la date d'indexation est antérieure à la date spécifiée.
	 *
	 * @param date       une date
	 * @param typesTiers les types de tiers à prendre en compte
	 */
	void deleteTiersIndexedBefore(@NotNull Date date, @NotNull Set<TypeTiers> typesTiers) {

		final Set<String> subTypes = typesTiers.stream()
				.map(GlobalTiersIndexerImpl::getIndexSubTypes)
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());

		try {
			// critère de recherche sur la date d'indexation (qui doit tomber dans le range [0..date[ )
			final Query dateQuery = NumericRangeQuery.newLongRange(TiersIndexableData.INDEXATION_DATE,
			                                                   null,
			                                                   date.getTime(),
			                                                   false,
			                                                   false);

			// critère de recherche sur le ou les types de contribuables
			final BooleanQuery typeQuery = new BooleanQuery();
			for (String s : subTypes) {
				typeQuery.add(new TermQuery(new Term(LuceneHelper.F_DOCSUBTYPE, s)), BooleanClause.Occur.SHOULD);
			}

			// les deux critères doivent être respectés
			final BooleanQuery fullQuery = new BooleanQuery();
			fullQuery.add(dateQuery, BooleanClause.Occur.MUST);
			fullQuery.add(typeQuery, BooleanClause.Occur.MUST);

			// on efface les tiers qui correspondent au critère complet
			globalIndex.deleteEntitiesMatching(fullQuery);
		}
		catch (LuceneException e) {
			throw new IndexerException(e);
		}
	}

	/**
	 * @param type un type de tiers
	 * @return le ou less sous-type d'indexation qui correspondent au type de tiers spécifié.
	 */
	@NotNull
	private static List<String> getIndexSubTypes(@NotNull TypeTiers type) {
		final List<String> sub = new ArrayList<>(2);
		switch (type) {
		case AUTRE_COMMUNAUTE:
			sub.add(AutreCommunauteIndexable.SUB_TYPE);
			break;
		case COLLECTIVITE_ADMINISTRATIVE:
			sub.add(CollectiviteAdministrativeIndexable.SUB_TYPE);
			break;
		case DEBITEUR_PRESTATION_IMPOSABLE:
			sub.add(DebiteurPrestationImposableIndexable.SUB_TYPE);
			break;
		case ENTREPRISE:
			sub.add(EntrepriseIndexable.SUB_TYPE);
			break;
		case ETABLISSEMENT:
			sub.add(EtablissementIndexable.SUB_TYPE);
			break;
		case MENAGE_COMMUN:
			sub.add(MenageCommunIndexable.SUB_TYPE);
			break;
		case PERSONNE_PHYSIQUE:
			sub.add(HabitantIndexable.SUB_TYPE);
			sub.add(NonHabitantIndexable.SUB_TYPE);
			break;
		default:
			throw new IllegalArgumentException("Type de tiers inconnu = [" + type + "]");
		}
		return sub;
	}

	private DeltaIds getIdsToIndex(final Mode mode, @NotNull Set<TypeTiers> typesTiers) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(status -> {

			final DeltaIds deltaIds;
			switch (mode) {
			case FULL:
			case FULL_INCREMENTAL:
			{
				// JDE : on traite les identifiants dans l'ordre décroissant pour traiter les PP d'abord...
				final List<Long> allIds = new ArrayList<>(tiersDAO.getAllIdsFor(true, typesTiers));
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

	private int indexMultithreads(List<Long> list, int nbThreads, @NotNull Mode mode, StatusManager statusManager) throws Exception {

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
	protected MassTiersIndexer createMassTiersIndexer(int nbThreads, @NotNull Mode mode, int queueSizeByThread) {
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
	    if (tiers == null) {
		    throw new IllegalArgumentException();
	    }

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
	            if (numeroIndividu == null) {
		            throw new IllegalArgumentException();
	            }
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
