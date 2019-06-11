package ch.vd.unireg.indexer.tiers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.audit.AuditManager;
import ch.vd.unireg.avatar.AvatarService;
import ch.vd.unireg.cache.ServiceCivilCacheWarmer;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.Switchable;
import ch.vd.unireg.common.ThreadSwitch;
import ch.vd.unireg.indexer.GlobalIndexInterface;
import ch.vd.unireg.indexer.IndexableData;
import ch.vd.unireg.indexer.IndexerBatchException;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.indexer.async.OnTheFlyTiersIndexer;
import ch.vd.unireg.indexer.jobs.DatabaseIndexationProcessor;
import ch.vd.unireg.indexer.jobs.DatabaseIndexationResults;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.interfaces.service.ServiceEntreprise;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.load.BasicLoadMonitor;
import ch.vd.unireg.load.LoadAverager;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.stats.LoadMonitorable;
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

public class GlobalTiersIndexerImpl implements GlobalTiersIndexer, InitializingBean, DisposableBean {

	public static final int NANO_TO_MILLI = 1000000;

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
	private ServiceEntreprise serviceEntreprise;
	private AssujettissementService assujettissementService;
	private AvatarService avatarService;
	private StatsService statsService;
	private AuditManager audit;
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

	@Override
	public int indexAllDatabase() throws IndexerException {
		return indexAllDatabase(Mode.FULL, 1, null);
	}

	@Override
	public int indexAllDatabase(@NotNull Mode mode, int nbThreads, @Nullable StatusManager statusManager) throws IndexerException {
    	// on prends en compte toute la population
		final EnumSet<TypeTiers> typesTiers = EnumSet.allOf(TypeTiers.class);

		final DatabaseIndexationProcessor processor = new DatabaseIndexationProcessor(tiersDAO, serviceCivilCacheWarmer, statsService, tiersSearcher, this, globalIndex, sessionFactory, transactionManager, audit);
		final DatabaseIndexationResults results = processor.run(mode, typesTiers, nbThreads, statusManager);
		return results.getIndexes().size();
	}

	@Override
	public void indexTiers(@NotNull List<Tiers> tiers, boolean removeBefore, boolean followDependents) throws IndexerBatchException {

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
            indexable = new DebiteurPrestationImposableIndexable(adresseService, tiersService, assujettissementService, serviceCivilService, serviceEntreprise, serviceInfra, avatarService, dpi);
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
	        indexable = new EntrepriseIndexable(adresseService, tiersService, assujettissementService, serviceInfra, serviceEntreprise, avatarService, entreprise);
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
	        indexable = new EtablissementIndexable(adresseService, tiersService, assujettissementService, serviceInfra, serviceEntreprise, avatarService, etablissement);
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

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void setTiersSearcher(GlobalTiersSearcher tiersSearcher) {
        this.tiersSearcher = tiersSearcher;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	public void setServiceEntreprise(ServiceEntreprise serviceEntreprise) {
		this.serviceEntreprise = serviceEntreprise;
	}

	public void setOnTheFlyTiersIndexer(OnTheFlyTiersIndexer onTheFlyTiersIndexer) {
		this.onTheFlyTiersIndexer = onTheFlyTiersIndexer;
	}

	public void setAudit(AuditManager audit) {
		this.audit = audit;
	}
}
