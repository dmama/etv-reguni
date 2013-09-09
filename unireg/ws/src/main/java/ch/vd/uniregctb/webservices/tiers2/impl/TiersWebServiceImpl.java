package ch.vd.uniregctb.webservices.tiers2.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.shared.batchtemplate.BatchResults;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BatchTransactionTemplateWithResults;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.source.ListeRecapService;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.indexer.EmptySearchCriteriaException;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.TooManyResultsIndexerException;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;
import ch.vd.uniregctb.jms.BamMessageHelper;
import ch.vd.uniregctb.jms.BamMessageSender;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersDAO.Parts;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.webservices.common.NoOfsTranslator;
import ch.vd.uniregctb.webservices.tiers2.TiersWebService;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiers;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.data.CodeQuittancement;
import ch.vd.uniregctb.webservices.tiers2.data.Date;
import ch.vd.uniregctb.webservices.tiers2.data.Debiteur;
import ch.vd.uniregctb.webservices.tiers2.data.DebiteurHisto;
import ch.vd.uniregctb.webservices.tiers2.data.DebiteurInfo;
import ch.vd.uniregctb.webservices.tiers2.data.DemandeQuittancementDeclaration;
import ch.vd.uniregctb.webservices.tiers2.data.EvenementPM;
import ch.vd.uniregctb.webservices.tiers2.data.MenageCommun;
import ch.vd.uniregctb.webservices.tiers2.data.MenageCommunHisto;
import ch.vd.uniregctb.webservices.tiers2.data.PersonneMorale;
import ch.vd.uniregctb.webservices.tiers2.data.PersonneMoraleHisto;
import ch.vd.uniregctb.webservices.tiers2.data.PersonnePhysique;
import ch.vd.uniregctb.webservices.tiers2.data.PersonnePhysiqueHisto;
import ch.vd.uniregctb.webservices.tiers2.data.ReponseQuittancementDeclaration;
import ch.vd.uniregctb.webservices.tiers2.data.Tiers;
import ch.vd.uniregctb.webservices.tiers2.data.Tiers.Type;
import ch.vd.uniregctb.webservices.tiers2.data.TiersHisto;
import ch.vd.uniregctb.webservices.tiers2.data.TiersId;
import ch.vd.uniregctb.webservices.tiers2.data.TiersInfo;
import ch.vd.uniregctb.webservices.tiers2.data.TiersPart;
import ch.vd.uniregctb.webservices.tiers2.exception.AccessDeniedException;
import ch.vd.uniregctb.webservices.tiers2.exception.BusinessException;
import ch.vd.uniregctb.webservices.tiers2.exception.TechnicalException;
import ch.vd.uniregctb.webservices.tiers2.exception.WebServiceException;
import ch.vd.uniregctb.webservices.tiers2.exception.WebServiceExceptionType;
import ch.vd.uniregctb.webservices.tiers2.impl.exception.QuittancementErreur;
import ch.vd.uniregctb.webservices.tiers2.params.AllConcreteTiersClasses;
import ch.vd.uniregctb.webservices.tiers2.params.GetBatchTiers;
import ch.vd.uniregctb.webservices.tiers2.params.GetBatchTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.params.GetDebiteurInfo;
import ch.vd.uniregctb.webservices.tiers2.params.GetListeCtbModifies;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiers;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiersPeriode;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiersType;
import ch.vd.uniregctb.webservices.tiers2.params.QuittancerDeclarations;
import ch.vd.uniregctb.webservices.tiers2.params.SearchEvenementsPM;
import ch.vd.uniregctb.webservices.tiers2.params.SearchTiers;
import ch.vd.uniregctb.webservices.tiers2.params.SetTiersBlocRembAuto;

public class TiersWebServiceImpl implements TiersWebService {

	private static final Logger LOGGER = Logger.getLogger(TiersWebServiceImpl.class);

	private static final int MAX_BATCH_SIZE = 500;
	// la limite Oracle est à 1'000, mais comme on peut recevoir des ménages communs, il faut garder une bonne marge pour charger les personnes physiques associées.

	private static final Set<CategorieImpotSource> CIS_SUPPORTEES = EnumHelper.getCategoriesImpotSourceAutorisees();

	private final Context context = new Context();

	private GlobalTiersSearcher tiersSearcher;
	private ExecutorService threadPool;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersDAO(TiersDAO tiersDAO) {
		context.tiersDAO = tiersDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersService(TiersService tiersService) {
		context.tiersService = tiersService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setSituationService(SituationFamilleService situationService) {
		context.situationService = situationService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setAdresseService(AdresseService adresseService) {
		context.adresseService = adresseService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setInfraService(ServiceInfrastructureService infraService) {
		context.infraService = infraService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setIbanValidator(IbanValidator ibanValidator) {
		context.ibanValidator = ibanValidator;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setParametreService(ParametreAppService parametreService) {
		context.parametreService = parametreService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersSearcher(GlobalTiersSearcher tiersSearcher) {
		this.tiersSearcher = tiersSearcher;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceCivil(ServiceCivilService service) {
		context.serviceCivilService = service;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setNoOfsTranslator(NoOfsTranslator translator) {
		context.noOfsTranslator = translator;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHibernateTemplate(HibernateTemplate template) {
		context.hibernateTemplate = template;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTransactionManager(PlatformTransactionManager manager) {
		context.transactionManager = manager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setLrService(ListeRecapService service) {
		context.lrService = service;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDiService(DeclarationImpotService service) {
		context.diService = service;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setBamMessageSender(BamMessageSender service) {
		context.bamSender = service;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServicePM(ServicePersonneMoraleService service) {
		context.servicePM = service;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setAssujettissementService(AssujettissementService service) {
		context.assujettissementService = service;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setPeriodeImpositionService(PeriodeImpositionService service) {
		context.periodeImpositionService = service;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setThreadPool(ExecutorService threadPool) {
		this.threadPool = threadPool;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public List<TiersInfo> searchTiers(SearchTiers params) throws BusinessException, AccessDeniedException, TechnicalException {

		try {
			Set<TiersInfo> set = new HashSet<>();

			final List<TiersCriteria> criteria = DataHelper.webToCore(params);
			for (TiersCriteria criterion : criteria) {
				if (criterion.isEmpty()) { // on évite de faire un appel distant pour rien
					throw new EmptySearchCriteriaException("Les critères de recherche sont vides");
				}
				final List<TiersIndexedData> values = tiersSearcher.search(criterion);
				for (TiersIndexedData value : values) {
					if (value != null && (value.getCategorieImpotSource() == null || CIS_SUPPORTEES.contains(value.getCategorieImpotSource()))) {
						final TiersInfo info = DataHelper.coreToWeb(value);
						set.add(info);
					}
				}
			}

			return new ArrayList<>(set);
		}
		catch (TooManyResultsIndexerException e) {
			throw new BusinessException(e);
		}
		catch (EmptySearchCriteriaException e) {
			throw new BusinessException(e);
		}
		catch (IndexerException e) {
			LOGGER.error(e, e);
			throw new BusinessException(e);
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw new TechnicalException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public Tiers getTiers(GetTiers params) throws BusinessException, AccessDeniedException, TechnicalException {

		try {
			final ch.vd.registre.base.date.RegDate date = RegDateHelper.get(Date.asJavaDate(params.date));
			Tiers data;

			final ch.vd.uniregctb.tiers.Tiers tiers = context.tiersService.getTiers(params.tiersNumber);
			if (tiers == null) {
				return null;
			}

			if (tiers instanceof ch.vd.uniregctb.tiers.PersonnePhysique) {
				final ch.vd.uniregctb.tiers.PersonnePhysique personne = (ch.vd.uniregctb.tiers.PersonnePhysique) tiers;
				BusinessHelper.warmIndividus(personne, params.parts, context);
				data = new PersonnePhysique(personne, params.parts, date, context);
			}
			else if (tiers instanceof ch.vd.uniregctb.tiers.MenageCommun) {
				final ch.vd.uniregctb.tiers.MenageCommun menage = (ch.vd.uniregctb.tiers.MenageCommun) tiers;
				BusinessHelper.warmIndividus(menage, params.parts, context);
				data = new MenageCommun(menage, params.parts, date, context);
			}
			else if (tiers instanceof ch.vd.uniregctb.tiers.Entreprise) {
				final Entreprise entreprise = (Entreprise) tiers;
				final PersonneMoraleHisto histo = new PersonneMoraleHisto(entreprise, params.parts, context);
				data = new PersonneMorale(histo, params.date);
			}
			else if (tiers instanceof ch.vd.uniregctb.tiers.DebiteurPrestationImposable) {
				final ch.vd.uniregctb.tiers.DebiteurPrestationImposable debiteur = (ch.vd.uniregctb.tiers.DebiteurPrestationImposable) tiers;
				data = new Debiteur(debiteur, params.parts, date, context);
			}
			else {
				data = null;
			}

			return data;
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw new TechnicalException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public TiersHisto getTiersPeriode(GetTiersPeriode params) throws BusinessException, AccessDeniedException, TechnicalException {

		try {
			final ch.vd.uniregctb.tiers.Tiers tiers = context.tiersService.getTiers(params.tiersNumber);
			if (tiers == null) {
				return null;
			}

			final TiersHisto data;
			if (tiers instanceof ch.vd.uniregctb.tiers.PersonnePhysique) {
				final ch.vd.uniregctb.tiers.PersonnePhysique personne = (ch.vd.uniregctb.tiers.PersonnePhysique) tiers;
				BusinessHelper.warmIndividus(personne, params.parts, context);
				data = new PersonnePhysiqueHisto(personne, params.periode, params.parts, context);
			}
			else if (tiers instanceof ch.vd.uniregctb.tiers.MenageCommun) {
				final ch.vd.uniregctb.tiers.MenageCommun menage = (ch.vd.uniregctb.tiers.MenageCommun) tiers;
				BusinessHelper.warmIndividus(menage, params.parts, context);
				data = new MenageCommunHisto(menage, params.periode, params.parts, context);
			}
			else if (tiers instanceof ch.vd.uniregctb.tiers.Entreprise) {
				final Entreprise entreprise = (Entreprise) tiers;
				final PersonneMoraleHisto histo = new PersonneMoraleHisto(entreprise, params.parts, context);
				data = new PersonneMoraleHisto(histo, params.periode);
			}
			else if (tiers instanceof ch.vd.uniregctb.tiers.DebiteurPrestationImposable) {
				final ch.vd.uniregctb.tiers.DebiteurPrestationImposable debiteur = (ch.vd.uniregctb.tiers.DebiteurPrestationImposable) tiers;
				data = new DebiteurHisto(debiteur, params.periode, params.parts, context);
			}
			else {
				data = null;
			}

			return data;
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw new TechnicalException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public TiersHisto getTiersHisto(GetTiersHisto params) throws BusinessException, AccessDeniedException, TechnicalException {

		try {
			final ch.vd.uniregctb.tiers.Tiers tiers = context.tiersService.getTiers(params.tiersNumber);
			if (tiers == null) {
				return null;
			}

			final TiersHisto data;
			if (tiers instanceof ch.vd.uniregctb.tiers.PersonnePhysique) {
				final ch.vd.uniregctb.tiers.PersonnePhysique personne = (ch.vd.uniregctb.tiers.PersonnePhysique) tiers;
				BusinessHelper.warmIndividus(personne, params.parts, context);
				data = new PersonnePhysiqueHisto(personne, params.parts, context);
			}
			else if (tiers instanceof ch.vd.uniregctb.tiers.MenageCommun) {
				final ch.vd.uniregctb.tiers.MenageCommun menage = (ch.vd.uniregctb.tiers.MenageCommun) tiers;
				BusinessHelper.warmIndividus(menage, params.parts, context);
				data = new MenageCommunHisto(menage, params.parts, context);
			}
			else if (tiers instanceof ch.vd.uniregctb.tiers.Entreprise) {
				final Entreprise entreprise = (Entreprise) tiers;
				data = new PersonneMoraleHisto(entreprise, params.parts, context);
			}
			else if (tiers instanceof ch.vd.uniregctb.tiers.DebiteurPrestationImposable) {
				final ch.vd.uniregctb.tiers.DebiteurPrestationImposable debiteur = (ch.vd.uniregctb.tiers.DebiteurPrestationImposable) tiers;
				data = new DebiteurHisto(debiteur, params.parts, context);
			}
			else {
				data = null;
			}

			return data;
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw new TechnicalException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BatchTiers getBatchTiers(final GetBatchTiers params) throws BusinessException, AccessDeniedException, TechnicalException {
		try {
			if (params.tiersNumbers == null || params.tiersNumbers.isEmpty()) {
				return new BatchTiers();
			}

			if (params.tiersNumbers.size() > MAX_BATCH_SIZE) {
				final String message = "La taille des requêtes batch ne peut pas dépasser " + MAX_BATCH_SIZE + '.';
				LOGGER.error(message);
				throw new BusinessException(message);
			}


			final ch.vd.registre.base.date.RegDate date = RegDateHelper.get(Date.asJavaDate(params.date));

			final Map<Long, Object> results = mapTiers(params.tiersNumbers, date, params.parts, new MapCallback() {
				@Override
				public Object map(ch.vd.uniregctb.tiers.Tiers tiers, Set<TiersPart> parts, RegDate date, Context context) {
					try {
						final Tiers t;
						if (tiers instanceof ch.vd.uniregctb.tiers.PersonnePhysique) {
							final ch.vd.uniregctb.tiers.PersonnePhysique personne = (ch.vd.uniregctb.tiers.PersonnePhysique) tiers;
							t = new PersonnePhysique(personne, parts, date, context);
						}
						else if (tiers instanceof ch.vd.uniregctb.tiers.MenageCommun) {
							final ch.vd.uniregctb.tiers.MenageCommun menage = (ch.vd.uniregctb.tiers.MenageCommun) tiers;
							t = new MenageCommun(menage, parts, date, context);
						}
						else if (tiers instanceof ch.vd.uniregctb.tiers.Entreprise) {
							final Entreprise entreprise = (Entreprise) tiers;
							final PersonneMoraleHisto histo = new PersonneMoraleHisto(entreprise, parts, context);
							t = new PersonneMorale(histo, params.date);
						}
						else if (tiers instanceof DebiteurPrestationImposable) {
							final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;
							t = new Debiteur(debiteur, parts, date, context);
						}
						else {
							t = null;
						}
						return t;
					}
					catch (WebServiceException e) {
						return e;
					}
					catch (RuntimeException e) {
						LOGGER.error(e, e);
						return new TechnicalException(e);
					}
				}
			});


			return new BatchTiers(results);
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw new TechnicalException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BatchTiersHisto getBatchTiersHisto(final GetBatchTiersHisto params) throws BusinessException, AccessDeniedException, TechnicalException {
		try {
			if (params.tiersNumbers == null || params.tiersNumbers.isEmpty()) {
				return new BatchTiersHisto();
			}

			if (params.tiersNumbers.size() > MAX_BATCH_SIZE) {
				final String message = "La taille des requêtes batch ne peut pas dépasser " + MAX_BATCH_SIZE + '.';
				LOGGER.error(message);
				throw new BusinessException(message);
			}

			final Map<Long, Object> results = mapTiers(params.tiersNumbers, null, params.parts, new MapCallback() {
				@Override
				public Object map(ch.vd.uniregctb.tiers.Tiers tiers, Set<TiersPart> parts, RegDate date, Context context) {
					try {
						final TiersHisto t;
						if (tiers instanceof ch.vd.uniregctb.tiers.PersonnePhysique) {
							final ch.vd.uniregctb.tiers.PersonnePhysique personne = (ch.vd.uniregctb.tiers.PersonnePhysique) tiers;
							t = new PersonnePhysiqueHisto(personne, parts, context);
						}
						else if (tiers instanceof ch.vd.uniregctb.tiers.MenageCommun) {
							final ch.vd.uniregctb.tiers.MenageCommun menage = (ch.vd.uniregctb.tiers.MenageCommun) tiers;
							t = new MenageCommunHisto(menage, parts, context);
						}
						else if (tiers instanceof ch.vd.uniregctb.tiers.Entreprise) {
							final Entreprise entreprise = (Entreprise) tiers;
							t = new PersonneMoraleHisto(entreprise, parts, context);
						}
						else if (tiers instanceof DebiteurPrestationImposable) {
							final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;
							t = new DebiteurHisto(debiteur, parts, context);
						}
						else {
							t = null;
						}
						return t;
					}
					catch (WebServiceException e) {
						return e;
					}
					catch (RuntimeException e) {
						LOGGER.error(e, e);
						return new TechnicalException(e);
					}
				}
			});


			return new BatchTiersHisto(results);
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw new TechnicalException(e);
		}
	}

	/**
	 * Cette méthode charge les tiers à partir de la base de données Unireg et les converti au format des Tiers du web-service.
	 *
	 * @param tiersNumbers les numéros de tiers à extraire.
	 * @param date         la date de validité des tiers (<b>null</b> pour la date courante)
	 * @param parts        les parties à renseigner sur les tiers
	 * @param callback     la méthode de callback qui va convertir chacuns des tiers de la base de données en tiers du web-service.
	 * @return une map contenant les tiers extraits, indexés par leurs numéros. Lorsqu'un tiers n'existe pas, la valeur associée à son id est nulle. En cas d'exception, la valeur associée à l'id est
	 *         l'exception elle-même.
	 */
	@SuppressWarnings({"unchecked"})
	private Map<Long, Object> mapTiers(Set<Long> tiersNumbers, @Nullable RegDate date, Set<TiersPart> parts, MapCallback callback) {

		final Set<Long> allIds = trim(tiersNumbers);

		final Map<Long, Object> results = new HashMap<>();
		long loadTiersTime = 0;
		long mapTiersTime = 0;

		// on découpe le travail sur plusieurs threads
		final int nbThreads = Math.max(1, Math.min(10, allIds.size() / 10)); // un thread pour chaque multiple de 10 tiers. Au minimum 1 thread, au maximum 10 threads

		if (nbThreads == 1) {
			// un seul thread, on utilise le thread courant
			final MappingThread t = new MappingThread(allIds, date, parts, context, callback);
			t.run();

			final RuntimeException e = t.getProcessingException();
			if (e != null) {
				throw new RuntimeException("Exception [" + e.getMessage() + "] dans le thread de mapping du getBatchTiers", e);
			}

			results.putAll(t.getResults());
			loadTiersTime += t.loadTiersTime;
			mapTiersTime += t.mapTiersTime;
		}
		else {
			// plusieurs threads, on délègue au thread pool
			final List<Set<Long>> list = split(allIds, nbThreads);

			// démarrage des threads
			final List<MappingThread> threads = new ArrayList<>(nbThreads);
			for (Set<Long> ids : list) {
				MappingThread t = new MappingThread(ids, date, parts, context, callback);
				threads.add(t);
				threadPool.execute(t);
			}

			// attente de la fin des threads
			for (MappingThread t : threads) {
				try {
					t.waitForProcessingDone();
				}
				catch (InterruptedException e) {
					// thread interrompu: il ne tourne plus, rien de spécial à faire en fait.
					LOGGER.warn("Le thread " + Thread.currentThread().getId() + " a été interrompu", e);
				}

				final RuntimeException e = t.getProcessingException();
				if (e != null) {
					throw new RuntimeException("Exception [" + e.getMessage() + "] dans le thread de mapping du getBatchTiers", e);
				}

				results.putAll(t.getResults());

				loadTiersTime += t.loadTiersTime;
				mapTiersTime += t.mapTiersTime;
			}
		}

		long totalTime = loadTiersTime + mapTiersTime;

		if (totalTime > 0 && LOGGER.isDebugEnabled()) {
			LOGGER.debug(String.format("temps d'exécution: chargement des tiers=%d%%, mapping des tiers=%d%%", loadTiersTime * 100 / totalTime, mapTiersTime * 100 / totalTime));
		}

		if (results.isEmpty()) {
			// aucun résultat, pas la peine d'aller plus loin.
			return null;
		}

		// ajoute les tiers non trouvés dans la base
		for (Long id : tiersNumbers) {
			if (!results.containsKey(id)) {
				results.put(id, null);
			}
		}

		return results;
	}

	/**
	 * Découpe le set d'ids en <i>n</i> sous-sets de tailles à peu près égales.
	 *
	 * @param allIds le set d'ids à découper
	 * @param n      le nombre de sous-sets voulu
	 * @return une liste de sous-sets
	 */
	private static List<Set<Long>> split(Set<Long> allIds, int n) {

		Iterator<Long> iter = allIds.iterator();
		int count = allIds.size() / n;

		List<Set<Long>> list = new ArrayList<>();

		for (int i = 0; i < n; i++) {
			Set<Long> ids = new HashSet<>();
			for (int j = 0; j < count && iter.hasNext(); j++) {
				ids.add(iter.next());
			}
			if (i == n - 1) { // le dernier prends tout ce qui reste
				while (iter.hasNext()) {
					ids.add(iter.next());
				}
			}
			list.add(ids);
		}
		return list;
	}

	/**
	 * @param input un set d'entrée
	 * @return une copie du set d'entrée avec toutes les valeurs nulles supprimées; ou le set d'entrée lui-même s'il ne contient pas de valeur nulle.
	 */
	private static Set<Long> trim(Set<Long> input) {
		if (input.contains(null)) {
			HashSet<Long> trimmed = new HashSet<>(input);
			trimmed.remove(null);
			return trimmed;
		}
		return input;
	}

	/**
	 * Converti les <i>parties</i> du web-service en <i>parties</i> de la couche business, en y ajoutant la partie des fors fiscaux.
	 *
	 * @param parts les parties du web-service
	 * @return les parties de la couche business.
	 */
	protected static Set<Parts> webToCoreWithForsFiscaux(Set<TiersPart> parts) {
		Set<Parts> coreParts = DataHelper.webToCore(parts);
		if (coreParts == null) {
			coreParts = new HashSet<>();
		}
		// les fors fiscaux sont nécessaires pour déterminer les dates de début et de fin d'activité.
		coreParts.add(Parts.FORS_FISCAUX);
// msi (30.09.2010) : en fait, cela pénalise trop fortement les tiers autre que débiteurs.		
//		// les adresses et les rapports-entre-tiers sont nécessaires pour calculer les raisons sociales des débiteurs
//		coreParts.add(Parts.ADRESSES);
//		coreParts.add(Parts.RAPPORTS_ENTRE_TIERS);

		return coreParts;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public Tiers.Type getTiersType(GetTiersType params) throws BusinessException, AccessDeniedException, TechnicalException {

		try {
			final ch.vd.uniregctb.tiers.Tiers tiers = context.tiersService.getTiers(params.tiersNumber);
			if (tiers == null) {
				return null;
			}

			final Type type = DataHelper.getType(tiers);
			if (type == null) {
				Assert.fail("Type de tiers inconnu = [" + tiers.getClass().getSimpleName());
			}

			return type;
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw new TechnicalException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void setTiersBlocRembAuto(final SetTiersBlocRembAuto params) throws BusinessException, AccessDeniedException, TechnicalException {

		try {
			final ch.vd.uniregctb.tiers.Tiers tiers = context.tiersService.getTiers(params.tiersNumber);
			if (tiers == null) {
				throw new BusinessException("Le tiers n°" + params.tiersNumber + " n'existe pas.");
			}

			tiers.setBlocageRemboursementAutomatique(params.blocage);
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw new TechnicalException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<EvenementPM> searchEvenementsPM(SearchEvenementsPM params) throws BusinessException, AccessDeniedException, TechnicalException {
		final List<ch.vd.uniregctb.interfaces.model.EvenementPM> list =
				context.servicePM.findEvenements(params.tiersNumber, params.codeEvenement, DataHelper.webToCore(params.dateMinimale), DataHelper.webToCore(params.dateMaximale));
		return DataHelper.events2web(list);
	}

	@Override
	@Transactional(readOnly = true)
	public DebiteurInfo getDebiteurInfo(GetDebiteurInfo params) throws BusinessException, AccessDeniedException, TechnicalException {

		try {
			final ch.vd.uniregctb.tiers.Tiers tiers = context.tiersService.getTiers(params.numeroDebiteur);
			if (tiers == null) {
				throw new BusinessException("Le tiers n°" + params.numeroDebiteur + " n'existe pas.");
			}
			if (!(tiers instanceof DebiteurPrestationImposable)) {
				throw new BusinessException("Le tiers n°" + params.numeroDebiteur + " n'est pas un débiteur.");
			}

			final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;

			// [UNIREG-2110] Détermine les LRs émises et celles manquantes
			final List<? extends DateRange> lrEmises = debiteur.getDeclarationsForPeriode(params.periodeFiscale, false);
			final List<DateRange> lrManquantes = context.lrService.findLRsManquantes(debiteur, RegDate.get(params.periodeFiscale, 12, 31), new ArrayList<DateRange>());

			DebiteurInfo info = new DebiteurInfo();
			info.numeroDebiteur = params.numeroDebiteur;
			info.periodeFiscale = params.periodeFiscale;
			info.nbLRsEmises = lrEmises.size();
			info.nbLRsTheorique = info.nbLRsEmises + count(lrManquantes, params.periodeFiscale);

			return info;
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw new TechnicalException(e);
		}
	}

	/**
	 * Classe interne au quittancement des déclarations d'impôt
	 */
	private static class QuittancementResults implements BatchResults<DemandeQuittancementDeclaration, QuittancementResults> {

		private final List<ReponseQuittancementDeclaration> reponses = new ArrayList<>();

		@Override
		public void addErrorException(DemandeQuittancementDeclaration element, Exception e) {
			if (e instanceof ValidationException) {
				reponses.add(new ReponseQuittancementDeclaration(element.key, (ValidationException) e, WebServiceExceptionType.BUSINESS));
			}
			else if (e instanceof RuntimeException) {
				reponses.add(new ReponseQuittancementDeclaration(element.key, (RuntimeException) e, WebServiceExceptionType.TECHNICAL));
			}
			else {
				reponses.add(new ReponseQuittancementDeclaration(element.key, new RuntimeException(e.getMessage(), e), WebServiceExceptionType.TECHNICAL));
			}
		}

		@Override
		public void addAll(QuittancementResults right) {
			this.reponses.addAll(right.getReponses());
		}

		public void addReponse(ReponseQuittancementDeclaration reponse) {
			this.reponses.add(reponse);
		}

		public List<ReponseQuittancementDeclaration> getReponses() {
			return reponses;
		}
	}

	@Override
	public List<ReponseQuittancementDeclaration> quittancerDeclarations(QuittancerDeclarations params) throws TechnicalException {

		try {
			final List<DemandeQuittancementDeclaration> demandes = params.demandes;
			final BatchTransactionTemplateWithResults<DemandeQuittancementDeclaration, QuittancementResults> template =
					new BatchTransactionTemplateWithResults<>(demandes, demandes.size(), Behavior.REPRISE_AUTOMATIQUE, context.transactionManager, null);
			final QuittancementResults rapportFinal = new QuittancementResults();
			template.execute(rapportFinal, new BatchWithResultsCallback<DemandeQuittancementDeclaration, QuittancementResults>() {

				@Override
				public QuittancementResults createSubRapport() {
					return new QuittancementResults();
				}

				@Override
				public boolean doInTransaction(List<DemandeQuittancementDeclaration> batch, QuittancementResults rapport) throws Exception {
					for (DemandeQuittancementDeclaration demande : batch) {
						final ReponseQuittancementDeclaration r = quittancerDeclaration(demande);
						rapport.addReponse(r);
					}
					return true;
				}
			}, null);
			return rapportFinal.getReponses();
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw new TechnicalException(e);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<TiersId> getListeCtbModifies(GetListeCtbModifies params) throws BusinessException, AccessDeniedException, TechnicalException {


		try {
			if (DateHelper.isAfter(params.dateDebutRecherche, params.dateFinRecherche)) {
				throw new BusinessException("La date de début de recherche " + params.dateDebutRecherche.toString() + " est après la date de fin " + params.dateFinRecherche);
			}
			final List<TiersId> listTiersId = new ArrayList<>();
			final List<Long> listCtb = context.tiersDAO.getListeCtbModifies(params.dateDebutRecherche, params.dateFinRecherche);
			for (Long numeroTiers : listCtb) {
				listTiersId.add(new TiersId(numeroTiers));
			}
			return listTiersId;
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw new TechnicalException(e);
		}
	}

	private ReponseQuittancementDeclaration quittancerDeclaration(DemandeQuittancementDeclaration demande) {
		ReponseQuittancementDeclaration r;
		try {
			r = traiterDemande(demande);
		}
		catch (QuittancementErreur e) {
			r = new ReponseQuittancementDeclaration(demande.key, e);
		}
		catch (ValidationException e) {
			LOGGER.error(e, e);
			r = new ReponseQuittancementDeclaration(demande.key, e, WebServiceExceptionType.BUSINESS);
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			r = new ReponseQuittancementDeclaration(demande.key, e, WebServiceExceptionType.TECHNICAL);
		}
		return r;
	}

	/**
	 * Traite une demande de quittancement de déclaration,
	 *
	 * @param demande la demande de quittancement à traiter
	 * @return la réponse de la demande de quittancement en cas de traitement effectué.
	 * @throws QuittancementErreur une erreur explicite en cas d'impossibilité d'effectuer le traitement.
	 */
	private ReponseQuittancementDeclaration traiterDemande(DemandeQuittancementDeclaration demande) throws QuittancementErreur {

		final ch.vd.uniregctb.tiers.Contribuable ctb = (Contribuable) context.tiersDAO.get(demande.key.ctbId);
		if (ctb == null) {
			throw new QuittancementErreur(CodeQuittancement.ERREUR_CTB_INCONNU, "Le contribuable est inconnu.");
		}

		if (ctb.getDernierForFiscalPrincipal() == null) {
			throw new QuittancementErreur(CodeQuittancement.ERREUR_ASSUJETTISSEMENT_CTB, "Le contribuable ne possède aucun for principal : il n'aurait pas dû recevoir de déclaration d'impôt.");
		}

		if (ctb.isDebiteurInactif()) {
			throw new QuittancementErreur(CodeQuittancement.ERREUR_CTB_DEBITEUR_INACTIF, "Le contribuable est un débiteur inactif : impossible de quittancer la déclaration.");
		}

		final DeclarationImpotOrdinaire declaration = findDeclaration(ctb, demande.key.periodeFiscale, demande.key.numeroSequenceDI);
		if (declaration == null) {
			throw new QuittancementErreur(CodeQuittancement.ERREUR_DECLARATION_INEXISTANTE, "La déclaration n'existe pas.");
		}

		if (declaration.isAnnule()) {
			throw new QuittancementErreur(CodeQuittancement.ERREUR_DECLARATION_ANNULEE, "La déclaration a été annulée entre-temps.");
		}

		final RegDate dateRetour = DataHelper.webToCore(demande.dateRetour);
		if (RegDateHelper.isBeforeOrEqual(dateRetour, declaration.getDateExpedition(), NullDateBehavior.EARLIEST)) {
			throw new QuittancementErreur(CodeQuittancement.ERREUR_DATE_RETOUR_INVALIDE,
					"La date de retour spécifiée (" + dateRetour + ") est avant la date d'envoi de la déclaration (" + declaration.getDateExpedition() + ").");
		}

		// envoie le quittancement au BAM
		sendQuittancementToBam(declaration, dateRetour);

		// La déclaration est correcte, on la quittance
		final String source = StringUtils.isBlank(demande.source) ? EtatDeclarationRetournee.SOURCE_CEDI : demande.source; // [SIFISC-1782] historiquement, seul le CEDI quittance par le web-service.
		context.diService.quittancementDI(ctb, declaration, dateRetour, source, true);
		Assert.isEqual(TypeEtatDeclaration.RETOURNEE, declaration.getDernierEtat().getEtat());

		return new ReponseQuittancementDeclaration(demande.key, CodeQuittancement.OK);
	}

	private void sendQuittancementToBam(DeclarationImpotOrdinaire di, RegDate dateQuittancement) {
		final long ctbId = di.getTiers().getNumero();
		final int annee = di.getPeriode().getAnnee();
		final int noSequence = di.getNumero();
		try {
			final Map<String, String> bamHeaders = BamMessageHelper.buildCustomBamHeadersForQuittancementDeclaration(di, dateQuittancement, null);
			final String businessId = String.format("%d-%d-%d-%s", ctbId, annee, noSequence, new SimpleDateFormat("yyyyMMddHHmmssSSS").format(DateHelper.getCurrentDate()));
			final String processDefinitionId = BamMessageHelper.PROCESS_DEFINITION_ID_PAPIER;       // pour le moment, tous les quittancements par le WS concenent les DI "papier"
			final String processInstanceId = BamMessageHelper.buildProcessInstanceId(di);
			context.bamSender.sendBamMessageQuittancementDi(processDefinitionId, processInstanceId, businessId, ctbId, annee, bamHeaders);
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(String.format("Erreur à la notification au BAM du quittancement de la DI %d (%d) du contribuable %d", annee, noSequence, ctbId), e);
		}
	}

	/**
	 * Recherche la declaration pour une année et un numéro de déclaration dans l'année
	 *
	 * @param contribuable     un contribuable
	 * @param annee            une période fiscale complète (ex. 2010)
	 * @param numeroSequenceDI le numéro de séquence de la déclaration pour le contribuable et la période considérés
	 * @return une déclaration d'impôt ordinaire, ou <b>null</b> si aucune déclaration correspondant aux critère n'est trouvée.
	 */
	private static DeclarationImpotOrdinaire findDeclaration(final Contribuable contribuable, final int annee, int numeroSequenceDI) {

		// [SIFISC-1227] Nous avons des cas où le numéro de séquence a été ré-utilisé après annulation d'une DI précédente
		// -> on essaie toujours de renvoyer la déclaration non-annulée qui correspond et, s'il n'y en a pas, de renvoyer
		// la dernière déclaration annulée trouvée

		DeclarationImpotOrdinaire declaration = null;
		DeclarationImpotOrdinaire declarationAnnuleeTrouvee = null;
		final List<Declaration> declarations = contribuable.getDeclarationsSorted();
		if (declarations != null && !declarations.isEmpty()) {
			for (Declaration d : declarations) {
				if (d.getPeriode().getAnnee() != annee) {
					continue;
				}
				final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) d;
				if (numeroSequenceDI == 0) {
					// Dans le cas où le numero dans l'année n'est pas spécifié on prend la dernière DI trouvée sur la période
					declaration = di;
				}
				else if (di.getNumero() == numeroSequenceDI) {
					if (di.isAnnule()) {
						declarationAnnuleeTrouvee = di;
					}
					else {
						declaration = di;
						break;
					}
				}
			}
		}

		return declaration != null ? declaration : declarationAnnuleeTrouvee;
	}

	/**
	 * Détermine le nombre de LRs existant dans la période fiscale spécifiée
	 *
	 * @param lrs     une liste de LRs
	 * @param periode une période fiscale
	 * @return le nombre de LRs trouvées
	 */
	private int count(List<? extends DateRange> lrs, int periode) {
		if (lrs == null || lrs.isEmpty()) {
			return 0;
		}
		int c = 0;
		for (DateRange lr : lrs) {
			if (lr.getDateDebut().year() == periode && lr.getDateFin().year() == periode) {
				c++;
			}
		}
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doNothing(AllConcreteTiersClasses dummy) {
	}
}
