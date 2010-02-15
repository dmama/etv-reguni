package ch.vd.uniregctb.indexer.tiers;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.BatchIterator;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ProgrammingException;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.indexer.*;
import ch.vd.uniregctb.indexer.async.AsyncTiersIndexer;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceTracingInterface;
import ch.vd.uniregctb.tiers.*;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class GlobalTiersIndexerImpl implements GlobalTiersIndexer {

    /**
     * Le nombre d'individus précharger à partir d'apireg et insérés dans le cache du service civil.
     * <p/>
     * Attention: il est nécessaire que le nombre d'éléments cachés par le cache du service civil (voir ehcache.xml) soit au minimum égal à
     * 4 * CIVIL_BATCH_SIZE (x2 parce que chaque individu génère une entrée pour lui-même et une entrée pour ses adresses; et x2 parce que
     * si la queue est pleine il faut prévoir assez de place dans le cas où apireg est très rapide et à déjà chargé toutes les données
     * suivantes).
     */
    private static final int CIVIL_BATCH_SIZE = 500;

    private static final int NANO_TO_MILLI = 1000000;

    private static final Logger LOGGER = Logger.getLogger(GlobalTiersIndexerImpl.class);

    private GlobalIndexInterface globalIndex;
    private GlobalTiersSearcher tiersSearcher;
    private TiersDAO tiersDAO;
    private PlatformTransactionManager transactionManager;
    private SessionFactory sessionFactory;
    private ServiceInfrastructureService serviceInfra;
    private ServiceCivilService serviceCivilService;
	private ServiceCivilService serviceCivilApireg; // le service apireg est pour l'instant le seul à supporter le chargement par lots des individus.

    private static class Behavior {
        public boolean onTheFlyIndexation = true;
        public boolean throwOnTheFlyException = true;
    }

    private final ThreadLocal<Behavior> byThreadBehavior = new ThreadLocal<Behavior>();

    /**
     * Le service qui fournit les adresses et autres
     */
    private AdresseService adresseService;

    private TiersService tiersService;

    public void overwriteIndex() {
        globalIndex.overwriteIndex();
    }

    public int getApproxDocCount() {
        return globalIndex.getApproxDocCount();
    }

    public int getExactDocCount() {
        return globalIndex.getExactDocCount();
    }

    public void flush() {
        globalIndex.flush();
    }

    public void indexTiers(final long id) throws IndexerException {
        TransactionTemplate tmpl = new TransactionTemplate(transactionManager);
        tmpl.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                Tiers tiers = tiersDAO.get(id);
                if (tiers != null) {
                    indexTiers(tiers);
                }
                return null;
            }
        });
    }

    /**
     * @see ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer#indexTiers(ch.vd.uniregctb.tiers.Tiers)
     */
    public void indexTiers(Tiers tiers) throws IndexerException {
        indexTiers(tiers, true, true);
    }

    /**
     * @see ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer#indexTiers(ch.vd.uniregctb.tiers.Tiers, boolean)
     */
    public void indexTiers(Tiers tiers, boolean removeBefore) throws IndexerException {
        indexTiers(tiers, removeBefore, true);
    }

    public int indexAllDatabase() throws IndexerException {
        return indexAllDatabase(true, null);
    }

    private void setMessage(StatusManager statusManager, String msg) {

        if (statusManager != null) {
            statusManager.setMessage(msg);
        }
    }

	private class TimeLog {
		public long startTime;
		public long startTimeInfra;
		public long startTimeCivil;
		public long startTimeApireg;
		public long startTimeIndex;
		public long endTime;
		public long endTimeInfra;
		public long endTimeCivil;
		public long endTimeApireg;
		public long endTimeIndex;
		public long indexerCpuTime;
		public long indexerExecTime;

		public void start() {
			startTime = System.nanoTime() / NANO_TO_MILLI;
			startTimeInfra = getNanoInfra() / NANO_TO_MILLI;
			startTimeCivil = getNanoCivil() / NANO_TO_MILLI;
			startTimeApireg = getNanoApireg() / NANO_TO_MILLI;
			startTimeIndex = getNanoIndex() / NANO_TO_MILLI;
		}

		public void end(AsyncTiersIndexer asyncIndexer) {
			endTime = System.nanoTime() / NANO_TO_MILLI;
			endTimeInfra = getNanoInfra() / NANO_TO_MILLI;
			endTimeCivil = getNanoCivil() / NANO_TO_MILLI;
			endTimeApireg = getNanoApireg() / NANO_TO_MILLI;
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
		long timeWaitApireg = endTimeApireg - startTimeApireg;
		long timeWaitIndex = endTimeIndex - startTimeIndex;
		long timeWaitAutres = timeWait - timeWaitInfra - timeWaitCivil -timeWaitApireg - timeWaitIndex;

            if (indexerExecTime == 0 || timeWait == 0) {
                LOGGER.debug("Statistiques d'indexation indisponibles !");
                return;
            }

			int percentCpu = (int) (100 * indexerCpuTime / indexerExecTime);
			int percentWait = 100 - percentCpu;
			int percentWaitInfra = (int) (100 * timeWaitInfra / timeWait);
			int percentWaitCivil = (int) (100 * timeWaitCivil / timeWait);
			int percentWaitApireg = (int) (100 * timeWaitApireg / timeWait);
			int percentWaitIndex = (int) (100 * timeWaitIndex / timeWait);
			int percentWaitAutres = 100 - percentWaitInfra - percentWaitCivil - percentWaitApireg - percentWaitIndex;

			LOGGER.info("Temps total d'exécution         : " + timeTotal + " ms");
			LOGGER.info("Temps 'exec' threads indexation : " + indexerExecTime + " ms");
			LOGGER.info("Temps 'cpu' threads indexation  : " + indexerCpuTime + " ms" + " (" + percentCpu + "%)");
			LOGGER.info("Temps 'wait' threads indexation : " + timeWait + " ms" + " (" + percentWait + "%)");
			LOGGER.info(" - service infrastructure       : "
					+ (timeWaitInfra == 0 ? "<indisponible>" : timeWaitInfra + " ms" + " (" + percentWaitInfra + "%)"));
			LOGGER.info(" - service civil                : "
					+ (timeWaitCivil == 0 ? "<indisponible>" : timeWaitCivil + " ms" + " (" + percentWaitCivil + "%)"));
			LOGGER.info(" - service apireg               : "
					+ (timeWaitApireg == 0 ? "<indisponible>" : timeWaitApireg + " ms" + " (" + percentWaitApireg + "%)"));
			LOGGER.info(" - indexer                      : "
					+ (timeWaitIndex == 0 ? "<indisponible>" : timeWaitIndex + " ms" + " (" + percentWaitIndex + "%)"));
			LOGGER.info(" - autre (scheduler, jdbc, ...) : " + timeWaitAutres + " ms" + " (" + percentWaitAutres + "%)");
		}

        private long getNanoCivil() {
            long timecivil = 0;
            if (serviceCivilService instanceof ServiceTracingInterface) {
                ServiceTracingInterface tracing = (ServiceTracingInterface) serviceCivilService;
                timecivil = tracing.getTotalTime();
            }
            return timecivil;
        }

		private long getNanoApireg() {
			long timeapireg = 0;
			if (serviceCivilApireg instanceof ServiceTracingInterface) {
				ServiceTracingInterface tracing = (ServiceTracingInterface) serviceCivilApireg;
				timeapireg = tracing.getTotalTime();
			}
			return timeapireg;
		}

		private long getNanoInfra() {
			long timeinfra = 0;
			if (serviceInfra instanceof ServiceTracingInterface) {
				ServiceTracingInterface tracing = (ServiceTracingInterface) serviceInfra;
				timeinfra = tracing.getTotalTime();
			}
			return timeinfra;
		}

        private long getNanoIndex() {
            long timeindex = 0;
            if (globalIndex instanceof ServiceTracingInterface) {
                ServiceTracingInterface tracing = (ServiceTracingInterface) globalIndex;
                timeindex = tracing.getTotalTime();
            }
            return timeindex;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int indexAllDatabaseAsync(StatusManager statusManager, int nbThreads, Mode mode, boolean prefetchIndividus)
            throws IndexerException {

        if (statusManager == null) {
            statusManager = new LoggingStatusManager(LOGGER, Level.DEBUG);
        }

        Audit.info("Réindexation de la base de données (mode = " + mode + ")");

        if (mode.equals(Mode.FULL)) {
            // Ecrase l'indexe lucene sur le disque local
            statusManager.setMessage("Efface le repertoire d'indexation");
            overwriteIndex();
        }

        statusManager.setMessage("Récupération des tiers à indexer...");
        final List<Long> list;
        switch (mode) {
            case FULL:
                list = tiersDAO.getAllIds();
                break;

            case DIRTY_ONLY:
                list = tiersDAO.getDirtyIds();
                break;

            case INCREMENTAL:
                list = getIncrementalIds();
                break;

            default:
                throw new ProgrammingException("Mode d'indexation inconnu = " + mode);
        }

        int nbIndexed = 0;
        try {
            LOGGER.info("ASYNC indexation de " + list.size() + " tiers par " + nbThreads + " threads en mode " + mode
                    + (prefetchIndividus ? " avec" : " sans") + " préchargement des individus");

            TimeLog timeLog = new TimeLog();
            timeLog.start();

            final int queueSizeByThread = CIVIL_BATCH_SIZE / nbThreads;
            AsyncTiersIndexer asyncIndexer = new AsyncTiersIndexer(this, transactionManager, sessionFactory, nbThreads, queueSizeByThread, mode);
            asyncIndexer.initialize();

            int size = list.size();

            // variables pour le log
            int i = 0;

            BatchIterator<Long> iter = new BatchIterator<Long>(list, CIVIL_BATCH_SIZE);
            while (iter.hasNext() && !statusManager.interrupted()) {

                Iterator<Long> batch = iter.next();

                final Set<Long> ids = new HashSet<Long>();
                while (batch.hasNext()) {
                    Long id = batch.next();
                    ids.add(id);
                }

                statusManager.setMessage("Indexation du tiers " + i + " sur " + size, (100 * i) / size);

                if (prefetchIndividus && serviceCivilService.isWarmable()) {
                    /*
                          * Si le service est chauffable, on précharge les individus en vrac pour améliorer les performances. Sans préchargement,
                          * chaque individu est obtenu séparemment à travers host-interface (= au minimum une requête par individu); et avec le
                          * préchargement on peut charger 500 individus en faisant une requête à apireg.
                          */
                    long start = System.nanoTime();

					final List<Long> numerosIndividus = tiersDAO.getNumerosIndividu(ids);
					if (!numerosIndividus.isEmpty()) { // on peut tomber sur une plage de tiers ne contenant pas d'habitant
						try {
							final List<Individu> individus = serviceCivilApireg.getIndividus(numerosIndividus, null, EnumAttributeIndividu.ADRESSES);
							serviceCivilService.warmCache(individus, null, EnumAttributeIndividu.ADRESSES);

                            long nanosecondes = System.nanoTime() - start;
                            LOGGER.info("=> Récupéré 500 individus en " + (nanosecondes / 1000000000L) + "s.");
                        }
                        catch (Exception e) {
                            LOGGER.error("Impossible de précharger le lot d'individus [" + numerosIndividus
                                    + "]. On continue avec host-interface pour ce lot.", e);
                        }
                    }
                }

                // Dispatching des tiers à indexer
                for (Long id : ids) {
                    if (statusManager.interrupted()) {
                        asyncIndexer.clearQueue();
                        break;
                    }

                    /*
                          * insère l'id dans la queue à indexer, mais de manière à pouvoir interrompre le processus si plus personne ne prélève
                          * de tiers dans la queue (p.a. si tous les threads d'indexations sont morts).
                          */
                    while (!asyncIndexer.offerTiersForIndexation(id, 10, TimeUnit.SECONDS) && !statusManager.interrupted()) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("La queue d'indexation est pleine, attente de 10 secondes...");
                        }
                    }
                    ++i;
                }
            }

            asyncIndexer.terminate();

            timeLog.end(asyncIndexer);
            timeLog.logStats();

            if (statusManager.interrupted()) {
                Audit.warn("L'indexation a été interrompue. Nombre de tiers réindexés = " + size);
            } else {
                Audit.success("L'indexation s'est terminée avec succès. Nombre de tiers réindexés = " + size);
            }

            nbIndexed = size;
        }
        catch (Exception e) {
            Audit.error("Erreur lors de l'indexation: " + e.getMessage());
            throw new IndexerException(e);
        }
        return nbIndexed;
    }

    /**
     * @return la liste des ids non-indexés
     */
    private List<Long> getIncrementalIds() {

        final List<Long> idsDb = tiersDAO.getAllIds();
        final Set<Long> idsIndex = tiersSearcher.getAllIds();

        List<Long> list = new ArrayList<Long>();
        for (Long id : idsDb) {
            if (!idsIndex.contains(id)) {
                list.add(id);
            }
        }

        return list;
    }

    /**
     * @see ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer#indexAllDatabase()
     */
    public int indexAllDatabase(boolean assertSameNumber, StatusManager statusManager) throws IndexerException {

        overwriteIndex();
        Assert.isTrue(globalIndex.getApproxDocCount() == 0);

        int nbIndexed = indexAllGetAll(assertSameNumber, statusManager);

        setMessage(statusManager, "Optimization de la base d'indexation...");
        globalIndex.optimize();

        LOGGER.info("Il y a " + globalIndex.getApproxDocCount() + " entities dans l'index.");
        setMessage(statusManager, "Indexation terminée. Il y a " + globalIndex.getApproxDocCount() + " documents dans la base d'indexation");

        return nbIndexed;
    }

    public void indexTiers(Tiers tiers, boolean removeBefore, boolean followDependents) throws IndexerException {
        Assert.notNull(tiers, "Le Tiers passé pour l'indexation est null...");

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Reindexation du Tiers " + tiers.getNumero() + " (remove=" + removeBefore + " couple=" + followDependents + ")");
        }

        final List<Indexable> indexables = buildIndexables(tiers, followDependents);
        indexIndexable(indexables, removeBefore);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Indexation du Tiers #" + tiers.getNumero() + " successful. " + globalIndex.getApproxDocCount()
                    + " entities dans l'index");
        }
    }

    public void indexTiers(List<Tiers> tiers, boolean removeBefore, boolean followDependents) throws IndexerBatchException {
        Assert.notNull(tiers);

        // Note : en cas d'exception, on continue le processing, on stocke les exceptions et on les lèves d'un seul coup à la fin
        IndexerBatchException exception = null;

        // construit la liste des indexables

        final List<Indexable> indexables = new ArrayList<Indexable>(tiers.size());
        for (Tiers t : tiers) {
            try {
                final List<Indexable> i = buildIndexables(t, followDependents);
                indexables.addAll(i);
            }
            catch (Exception e) {
                if (exception == null) {
                    exception = new IndexerBatchException();
                }
                exception.addException(t, e);
            }
        }

        // process les indexables
        try {
            indexIndexable(indexables, removeBefore);
        }
        catch (IndexerException e) {
            if (exception == null) {
                exception = new IndexerBatchException();
            }
            exception.addException(e.getTiers(), e);
        }
        catch (Exception e) {
            if (exception == null) {
                exception = new IndexerBatchException();
            }
            exception.addException(null, e);
        }

        if (exception != null) {
            throw exception;
        }
    }


    private List<Indexable> buildIndexables(Tiers tiers, boolean followDependents) {

        final List<Indexable> indexables = new ArrayList<Indexable>();

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
            if (rapports != null && rapports.size() > 0) {
                for (RapportEntreTiers r : rapports) {
                    MenageCommun menage = (MenageCommun) r.getObjet();
                    list.add(menage);
                }
            }
        }

        // MenageCommun
        else if (tiers instanceof MenageCommun) {
            List<RapportEntreTiers> rapports = TiersHelper.getRapportObjetHistoOfType(tiers, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
            if (rapports != null && rapports.size() > 0) {
                for (RapportEntreTiers r : rapports) {
                    PersonnePhysique pp = (PersonnePhysique) r.getSujet();
                    list.add(pp);
                }
            }
        }

        // Reindex les DPI si on a un CTB
        if (tiers instanceof Contribuable) {
            Contribuable ctb = (Contribuable) tiers;
            Set<DebiteurPrestationImposable> dpis = ctb.getDebiteursPrestationImposable();
            if (dpis != null) {
                for (DebiteurPrestationImposable dpi : dpis) {
                    list.add(dpi);
                }
            }
        }
        return list;
    }

    private Indexable buildIndexable(Tiers tiers) {

        final Indexable indexable;

        if (tiers instanceof DebiteurPrestationImposable) {
            DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;
            indexable = new DebiteurPrestationImposableIndexable(adresseService, tiersService, dpi);
        } else if (tiers instanceof PersonnePhysique) {
            PersonnePhysique pp = (PersonnePhysique) tiers;
            // Habitant
            if (pp.isHabitant()) {
                Long numeroIndividu = pp.getNumeroIndividu();
                Assert.notNull(numeroIndividu);
                // Recuperation de l'individu
                Individu individu = serviceCivilService.getIndividu(numeroIndividu, 2400, EnumAttributeIndividu.ADRESSES);
                Assert.notNull(individu, "Individu introuvable. Numero=" + numeroIndividu);
                indexable = new HabitantIndexable(adresseService, tiersService, pp, individu);
            }
            // NonHabitant
            else {
                indexable = new NonHabitantIndexable(adresseService, tiersService, pp);
            }
        } else if (tiers instanceof MenageCommun) {
            final MenageCommun cmc = (MenageCommun) tiers;
            indexable = new MenageCommunIndexable(adresseService, tiersService, cmc);
        } else if (tiers instanceof Entreprise) {
            Entreprise entreprise = (Entreprise) tiers;
            indexable = new EntrepriseIndexable(adresseService, tiersService, entreprise);
        } else if (tiers instanceof AutreCommunaute) {
            AutreCommunaute autreCommunaute = (AutreCommunaute) tiers;
            indexable = new AutreCommunauteIndexable(adresseService, tiersService, autreCommunaute);
        } else if (tiers instanceof CollectiviteAdministrative) {
            CollectiviteAdministrative collectivite = (CollectiviteAdministrative) tiers;
            indexable = new CollectiviteAdministrativeIndexable(adresseService, tiersService, collectivite);
        } else {
            String message = "Le Tiers " + tiers.getNatureTiers() + " n'est pas connu de l'indexation!!!";
            LOGGER.error(message);
            throw new IndexerException(tiers, message);
        }

        return indexable;
    }

    private int indexAllGetAll(boolean assertSameNumber, final StatusManager statusManager) {

        TransactionTemplate tmpl = new TransactionTemplate(transactionManager);
        tmpl.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
        int nbTiersIndexed = (Integer) tmpl.execute(new TransactionCallback() {

            public Object doInTransaction(TransactionStatus status) {
                // Get the sourcier from DB
                List<Tiers> listTiers = tiersDAO.getAll();
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Indexation de " + listTiers.size() + " tiers");
                }
                int nbTiersIndexed = 0;
                for (Tiers tiers : listTiers) {

                    if (statusManager != null && statusManager.interrupted()) {
                        break;
                    }

                    setMessage(statusManager, "Indexation du tiers " + nbTiersIndexed + " sur " + listTiers.size() + " : "
                            + formatPercent(nbTiersIndexed, listTiers.size()) + " (id=" + tiers.getNumero() + ")");

                    try {
                        indexTiers(tiers, false, false);
                        nbTiersIndexed++;
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("Indexation du contribuable " + tiers.getNumero() + " terminée");
                        }
                    }
                    catch (Exception e) {
                        tiers.setIndexDirty(true);
                        LOGGER.error(e, e);
                    }

                    tiersDAO.evict(tiers);
                }
                return nbTiersIndexed;
            }

        });

        globalIndex.flush();

        int c = globalIndex.getExactDocCount();
        Assert.isTrue(!assertSameNumber || nbTiersIndexed == c, "Le nombre d'entités dans la base de données (" + nbTiersIndexed
                + ") n'est pas le même que dans l'indexer (" + c + ")");

        return nbTiersIndexed;
    }

    private void indexIndexable(List<Indexable> indexables, boolean removeBefore) throws IndexerException {

        final List<IndexableData> data = new ArrayList<IndexableData>(indexables.size());
        for (Indexable i : indexables) {
            data.add(new IndexableData(i));
        }

        if (removeBefore) {
            globalIndex.removeThenIndexEntities(data);
        } else {
            globalIndex.indexEntities(data);
        }
    }

    public void removeEntity(Long id, String type) {
        globalIndex.removeEntity(id, type);
    }

    private String formatPercent(int num, int denom) {
        return String.format("%d%%", (int) (num / 1.0f / denom * 100));
    }

    private Behavior getByThreadBehavior() {
        Behavior behavior = this.byThreadBehavior.get();
        if (behavior == null) {
            behavior = new Behavior();
            this.byThreadBehavior.set(behavior);
        }
        return behavior;
    }

    public boolean isOnTheFlyIndexation() {
        return getByThreadBehavior().onTheFlyIndexation;
    }

    public void setOnTheFlyIndexation(boolean onTheFlyIndexation) {
        getByThreadBehavior().onTheFlyIndexation = onTheFlyIndexation;
    }

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

	public void setServiceCivilApireg(ServiceCivilService serviceCivilApireg) {
		this.serviceCivilApireg = serviceCivilApireg;
	}

    public AdresseService getAdresseService() {
        return adresseService;
    }

    public void setAdresseService(AdresseService adresseService) {
        this.adresseService = adresseService;
    }

    public boolean isThrowOnTheFlyException() {
        return getByThreadBehavior().throwOnTheFlyException;
    }

    public void setThrowOnTheFlyException(boolean want) {
        getByThreadBehavior().throwOnTheFlyException = want;
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
}
