package ch.vd.uniregctb.webservices.party3.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import ch.vd.unireg.webservices.party3.AcknowledgeTaxDeclarationRequest;
import ch.vd.unireg.webservices.party3.AcknowledgeTaxDeclarationResponse;
import ch.vd.unireg.webservices.party3.AcknowledgeTaxDeclarationsRequest;
import ch.vd.unireg.webservices.party3.AcknowledgeTaxDeclarationsResponse;
import ch.vd.unireg.webservices.party3.BatchParty;
import ch.vd.unireg.webservices.party3.ExtendDeadlineCode;
import ch.vd.unireg.webservices.party3.ExtendDeadlineRequest;
import ch.vd.unireg.webservices.party3.ExtendDeadlineResponse;
import ch.vd.unireg.webservices.party3.GetBatchPartyRequest;
import ch.vd.unireg.webservices.party3.GetDebtorInfoRequest;
import ch.vd.unireg.webservices.party3.GetModifiedTaxpayersRequest;
import ch.vd.unireg.webservices.party3.GetPartyRequest;
import ch.vd.unireg.webservices.party3.GetPartyTypeRequest;
import ch.vd.unireg.webservices.party3.GetTaxOfficesRequest;
import ch.vd.unireg.webservices.party3.GetTaxOfficesResponse;
import ch.vd.unireg.webservices.party3.PartyNumberList;
import ch.vd.unireg.webservices.party3.PartyWebService;
import ch.vd.unireg.webservices.party3.SearchCorporationEventsRequest;
import ch.vd.unireg.webservices.party3.SearchCorporationEventsResponse;
import ch.vd.unireg.webservices.party3.SearchPartyRequest;
import ch.vd.unireg.webservices.party3.SearchPartyResponse;
import ch.vd.unireg.webservices.party3.SetAutomaticReimbursementBlockingRequest;
import ch.vd.unireg.webservices.party3.TaxDeclarationAcknowledgeCode;
import ch.vd.unireg.webservices.party3.WebServiceException;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.unireg.xml.exception.v1.TechnicalExceptionInfo;
import ch.vd.unireg.xml.party.debtor.v1.DebtorInfo;
import ch.vd.unireg.xml.party.taxdeclaration.v1.TaxDeclarationKey;
import ch.vd.unireg.xml.party.v1.Party;
import ch.vd.unireg.xml.party.v1.PartyInfo;
import ch.vd.unireg.xml.party.v1.PartyType;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.avatar.TypeAvatar;
import ch.vd.uniregctb.common.BatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.source.ListeRecapService;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.indexer.EmptySearchCriteriaException;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.TooManyResultsIndexerException;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.jms.BamMessageHelper;
import ch.vd.uniregctb.jms.BamMessageSender;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.metier.bouclement.ExerciceCommercialHelper;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.regimefiscal.RegimeFiscalService;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.NumerosOfficesImpot;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersDAO.Parts;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.EtatDelaiDeclaration;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.webservices.party3.data.AcknowledgeTaxDeclarationBuilder;
import ch.vd.uniregctb.webservices.party3.data.BatchPartyBuilder;
import ch.vd.uniregctb.webservices.party3.data.DebtorInfoBuilder;
import ch.vd.uniregctb.webservices.party3.data.ExtendDeadlineBuilder;
import ch.vd.uniregctb.webservices.party3.exception.ExtendDeadlineError;
import ch.vd.uniregctb.webservices.party3.exception.TaxDeclarationAcknowledgeError;
import ch.vd.uniregctb.xml.BusinessHelper;
import ch.vd.uniregctb.xml.Context;
import ch.vd.uniregctb.xml.ServiceException;
import ch.vd.uniregctb.xml.party.v1.PartyBuilder;

public class PartyWebServiceImpl implements PartyWebService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PartyWebServiceImpl.class);

	private static final int MAX_BATCH_SIZE = 500;
	// la limite Oracle est à 1'000, mais comme on peut recevoir des ménages communs, il faut garder une bonne marge pour charger les personnes physiques associées.

	private static final Set<CategorieImpotSource> CIS_SUPPORTEES = EnumHelper.getCategoriesImpotSourceAutorisees();

	private static final Set<TypeAvatar> TA_IGNORES = EnumHelper.getTypesAvatarsIgnores();

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
	public void setExerciceCommercialHelper(ExerciceCommercialHelper exerciceCommercialHelper) {
		context.exerciceCommercialHelper = exerciceCommercialHelper;
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
	public void setServiceOrganisation(ServiceOrganisationService service) {
		context.serviceOrganisationService = service;
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

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		context.evenementFiscalService = evenementFiscalService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRegimeFiscalService(RegimeFiscalService regimeFiscalService) {
		context.regimeFiscalService = regimeFiscalService;
	}

	@Override
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public SearchPartyResponse searchParty(SearchPartyRequest params) throws WebServiceException {

		try {
			Set<PartyInfo> set = new HashSet<>();

			final List<TiersCriteria> criteria = DataHelper.webToCore(params);
			for (TiersCriteria criterion : criteria) {
				if (criterion.isEmpty()) { // on évite de faire un appel distant pour rien
					throw new EmptySearchCriteriaException("Les critères de recherche sont vides");
				}
				final List<TiersIndexedData> values = tiersSearcher.search(criterion);
				for (TiersIndexedData value : values) {
					if (value != null
							&& (value.getCategorieImpotSource() == null || CIS_SUPPORTEES.contains(value.getCategorieImpotSource()))
							&& (value.getTypeAvatar() == null || !TA_IGNORES.contains(value.getTypeAvatar()))) {
						final PartyInfo info = ch.vd.uniregctb.xml.DataHelper.coreToXMLv1(value);
						set.add(info);
					}
				}
			}

			SearchPartyResponse array = new SearchPartyResponse();
			array.getItems().addAll(set);
			return array;
		}
		catch (TooManyResultsIndexerException e) {
			throw ExceptionHelper.newBusinessException(e, BusinessExceptionCode.INDEXER_TOO_MANY_RESULTS);
		}
		catch (EmptySearchCriteriaException e) {
			throw ExceptionHelper.newBusinessException(e, BusinessExceptionCode.INDEXER_EMPTY_CRITERIA);
		}
		catch (IndexerException e) {
			LOGGER.error(e.getMessage(), e);
			throw ExceptionHelper.newBusinessException(e, BusinessExceptionCode.INDEXER);
		}
		catch (RuntimeException e) {
			LOGGER.error(e.getMessage(), e);
			throw ExceptionHelper.newTechnicalException(e);
		}
	}

	@Override
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public Party getParty(GetPartyRequest params) throws WebServiceException {

		try {
			final ch.vd.uniregctb.tiers.Tiers tiers = context.tiersService.getTiers(params.getPartyNumber());
			if (tiers == null) {
				return null;
			}

			final Party data;
			final Set<ch.vd.unireg.xml.party.v1.PartyPart> parts = DataHelper.webToXML(params.getParts());
			if (tiers instanceof ch.vd.uniregctb.tiers.PersonnePhysique) {
				final ch.vd.uniregctb.tiers.PersonnePhysique personne = (ch.vd.uniregctb.tiers.PersonnePhysique) tiers;
				BusinessHelper.warmIndividusV1(personne, parts, context);
				data = PartyBuilder.newNaturalPerson(personne, parts, context);
			}
			else if (tiers instanceof ch.vd.uniregctb.tiers.MenageCommun) {
				final ch.vd.uniregctb.tiers.MenageCommun menage = (ch.vd.uniregctb.tiers.MenageCommun) tiers;
				BusinessHelper.warmIndividusV1(menage, parts, context);
				data = PartyBuilder.newCommonHousehold(menage, parts, context);
			}
			else if (tiers instanceof Entreprise) {
				final Entreprise entreprise = (Entreprise) tiers;
				data = PartyBuilder.newCorporation(entreprise, parts, context);
			}
			else if (tiers instanceof DebiteurPrestationImposable) {
				final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;
				data = PartyBuilder.newDebtor(debiteur, parts, context);
			}
			else if (tiers instanceof CollectiviteAdministrative) {
				final CollectiviteAdministrative coladm = (CollectiviteAdministrative) tiers;
				data = PartyBuilder.newAdministrativeAuthority(coladm, parts, context);
			}
			else {
				data = null;
			}

			return data;
		}
		catch (RuntimeException e) {
			LOGGER.error(e.getMessage(), e);
			throw ExceptionHelper.newTechnicalException(e);
		}
		catch (ServiceException e) {
			throw ExceptionHelper.newException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BatchParty getBatchParty(final GetBatchPartyRequest params) throws WebServiceException {
		try {
			final List<Integer> partyNumbers = params.getPartyNumbers();
			if (partyNumbers == null || partyNumbers.isEmpty()) {
				return new BatchParty();
			}

			if (partyNumbers.size() > MAX_BATCH_SIZE) {
				final String message = "La taille des requêtes batch ne peut pas dépasser " + MAX_BATCH_SIZE + '.';
				LOGGER.error(message);
				throw ExceptionHelper.newBusinessException(message, BusinessExceptionCode.INVALID_REQUEST);
			}

			final Map<Long, Object> results = mapParties(toLongSet(partyNumbers), null, DataHelper.webToXML(params.getParts()), new MapCallback() {
				@Override
				public Object map(ch.vd.uniregctb.tiers.Tiers tiers, Set<ch.vd.unireg.xml.party.v1.PartyPart> parts, RegDate date, Context context) {
					try {
						final Party t;
						if (tiers instanceof ch.vd.uniregctb.tiers.PersonnePhysique) {
							final ch.vd.uniregctb.tiers.PersonnePhysique personne = (ch.vd.uniregctb.tiers.PersonnePhysique) tiers;
							t = PartyBuilder.newNaturalPerson(personne, parts, context);
						}
						else if (tiers instanceof ch.vd.uniregctb.tiers.MenageCommun) {
							final ch.vd.uniregctb.tiers.MenageCommun menage = (ch.vd.uniregctb.tiers.MenageCommun) tiers;
							t = PartyBuilder.newCommonHousehold(menage, parts, context);
						}
						else if (tiers instanceof Entreprise) {
							final Entreprise entreprise = (Entreprise) tiers;
							t = PartyBuilder.newCorporation(entreprise, parts, context);
						}
						else if (tiers instanceof DebiteurPrestationImposable) {
							final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;
							t = PartyBuilder.newDebtor(debiteur, parts, context);
						}
						else if (tiers instanceof CollectiviteAdministrative) {
							final CollectiviteAdministrative coladm = (CollectiviteAdministrative) tiers;
							t = PartyBuilder.newAdministrativeAuthority(coladm, parts, context);
						}
						else {
							t = null;
						}
						return t;
					}
					catch (ServiceException e) {
						return ExceptionHelper.newException(e);
					}
					catch (RuntimeException e) {
						LOGGER.error(e.getMessage(), e);
						return ExceptionHelper.newTechnicalException(e);
					}
				}
			});

			return BatchPartyBuilder.newBatchParty(results);
		}
		catch (RuntimeException e) {
			LOGGER.error(e.getMessage(), e);
			throw ExceptionHelper.newTechnicalException(e);
		}
	}

	private static HashSet<Long> toLongSet(List<Integer> partyNumbers) {
		final HashSet<Long> numbers = new HashSet<>(partyNumbers.size());
		for (Integer n : partyNumbers) {
			if (n != null) {
				numbers.add(n.longValue());
			}
		}
		return numbers;
	}

	/**
	 * Cette méthode charge les tiers à partir de la base de données Unireg et les converti au format des Tiers du web-service.
	 *
	 * @param partyNumbers les numéros de tiers à extraire.
	 * @param date         la date de validité des tiers (<b>null</b> pour la date courante)
	 * @param parts        les parties à renseigner sur les tiers
	 * @param callback     la méthode de callback qui va convertir chacuns des tiers de la base de données en tiers du web-service.
	 * @return une map contenant les tiers extraits, indexés par leurs numéros. Lorsqu'un tiers n'existe pas, la valeur associée à son id est nulle. En cas d'exception, la valeur associée à l'id est
	 *         l'exception elle-même.
	 */
	@SuppressWarnings({"unchecked"})
	private Map<Long, Object> mapParties(Set<Long> partyNumbers, @Nullable RegDate date, Set<ch.vd.unireg.xml.party.v1.PartyPart> parts, MapCallback callback) {

		final Set<Long> allIds = trim(partyNumbers);

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
				throw new RuntimeException("Exception [" + e.getMessage() + "] dans le thread de mapping du getBatchParty", e);
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
					throw new RuntimeException("Exception [" + e.getMessage() + "] dans le thread de mapping du getBatchParty", e);
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
		for (Long id : partyNumbers) {
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
	protected static Set<Parts> xmlToCoreWithForsFiscaux(Set<ch.vd.unireg.xml.party.v1.PartyPart> parts) {
		Set<Parts> coreParts = ch.vd.uniregctb.xml.DataHelper.xmlToCoreV1(parts);
		if (coreParts == null) {
			coreParts = EnumSet.noneOf(Parts.class);
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
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public PartyType getPartyType(GetPartyTypeRequest params) throws WebServiceException {

		try {
			final ch.vd.uniregctb.tiers.Tiers tiers = context.tiersService.getTiers(params.getPartyNumber());
			if (tiers == null) {
				return null;
			}

			final PartyType type = DataHelper.getPartyType(tiers);
			if (type == null) {
				return null;
			}

			return type;
		}
		catch (RuntimeException e) {
			LOGGER.error(e.getMessage(), e);
			throw ExceptionHelper.newTechnicalException(e);
		}
	}

	@Override
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public GetTaxOfficesResponse getTaxOffices(GetTaxOfficesRequest params) throws WebServiceException {
		try {
			final NumerosOfficesImpot offices = context.tiersService.getOfficesImpot(params.getMunicipalityFSOId(), ch.vd.uniregctb.xml.DataHelper.xmlToCore(params.getDate()));
			if (offices == null) {
				return null;
			}

			return new GetTaxOfficesResponse((int) offices.getOid(), (int) offices.getOir(), null);
		}
		catch (RuntimeException e) {
			LOGGER.error(e.getMessage(), e);
			throw ExceptionHelper.newTechnicalException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void setAutomaticReimbursementBlocking(final SetAutomaticReimbursementBlockingRequest params) throws WebServiceException {

		try {
			final ch.vd.uniregctb.tiers.Tiers tiers = context.tiersService.getTiers(params.getPartyNumber());
			// [SIPM] Les établissements étaient complètement ignorés avant la v6 (= en fait, il n'y en avait pas, mais maintenant, ils arrivent...)
			if (tiers == null || tiers instanceof Etablissement) {
				throw ExceptionHelper.newBusinessException("Le tiers n°" + params.getPartyNumber() + " n'existe pas.", BusinessExceptionCode.UNKNOWN_PARTY);
			}

			tiers.setBlocageRemboursementAutomatique(params.isBlocked());
		}
		catch (RuntimeException e) {
			LOGGER.error(e.getMessage(), e);
			throw ExceptionHelper.newTechnicalException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SearchCorporationEventsResponse searchCorporationEvents(SearchCorporationEventsRequest params) throws WebServiceException {
		// TODO [SIPM] Remettre quelque chose ???
		return new SearchCorporationEventsResponse();
	}

	@Override
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public DebtorInfo getDebtorInfo(GetDebtorInfoRequest params) throws WebServiceException {

		try {
			final ch.vd.uniregctb.tiers.Tiers tiers = context.tiersService.getTiers(params.getDebtorNumber());
			if (tiers == null) {
				throw ExceptionHelper.newBusinessException("Le tiers n°" + params.getDebtorNumber() + " n'existe pas.", BusinessExceptionCode.UNKNOWN_PARTY);
			}
			if (!(tiers instanceof DebiteurPrestationImposable)) {
				throw ExceptionHelper.newBusinessException("Le tiers n°" + params.getDebtorNumber() + " n'est pas un débiteur.", BusinessExceptionCode.INVALID_PARTY_TYPE);
			}

			final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;

			// [UNIREG-2110] Détermine les LRs émises et celles manquantes
			final List<? extends DateRange> lrEmises = debiteur.getDeclarationsDansPeriode(DeclarationImpotSource.class, params.getTaxPeriod(), false);
			final List<DateRange> lrManquantes = context.lrService.findLRsManquantes(debiteur, RegDate.get(params.getTaxPeriod(), 12, 31), new ArrayList<>());

			return DebtorInfoBuilder.newDebtorInfo(params, lrEmises, lrManquantes);
		}
		catch (RuntimeException e) {
			LOGGER.error(e.getMessage(), e);
			throw ExceptionHelper.newTechnicalException(e);
		}
	}

	/**
	 * Classe interne au quittancement des déclarations d'impôt
	 */
	private static class AcknowledgeTaxDeclarationResults implements BatchResults<AcknowledgeTaxDeclarationRequest, AcknowledgeTaxDeclarationResults> {

		private final AcknowledgeTaxDeclarationsResponse reponses = new AcknowledgeTaxDeclarationsResponse();

		@Override
		public void addErrorException(AcknowledgeTaxDeclarationRequest element, Exception e) {
			if (e instanceof ValidationException) {
				reponses.getResponses().add(new AcknowledgeTaxDeclarationResponse(element.getKey(), TaxDeclarationAcknowledgeCode.EXCEPTION,
						new BusinessExceptionInfo(e.getMessage(), BusinessExceptionCode.VALIDATION.name(), null)));
			}
			else if (e instanceof RuntimeException) {
				reponses.getResponses().add(new AcknowledgeTaxDeclarationResponse(element.getKey(), TaxDeclarationAcknowledgeCode.EXCEPTION, new TechnicalExceptionInfo(e.getMessage(), null)));
			}
			else {
				reponses.getResponses().add(new AcknowledgeTaxDeclarationResponse(element.getKey(), TaxDeclarationAcknowledgeCode.EXCEPTION, new TechnicalExceptionInfo(e.getMessage(), null)));
			}
		}

		@Override
		public void addAll(AcknowledgeTaxDeclarationResults right) {
			this.reponses.getResponses().addAll(right.getReponses().getResponses());
		}

		public void addReponse(AcknowledgeTaxDeclarationResponse reponse) {
			this.reponses.getResponses().add(reponse);
		}

		public AcknowledgeTaxDeclarationsResponse getReponses() {
			return reponses;
		}
	}

	@Override
	public AcknowledgeTaxDeclarationsResponse acknowledgeTaxDeclarations(AcknowledgeTaxDeclarationsRequest params) throws WebServiceException {

		try {
			final List<AcknowledgeTaxDeclarationRequest> requests = params.getRequests();
			final BatchTransactionTemplateWithResults<AcknowledgeTaxDeclarationRequest, AcknowledgeTaxDeclarationResults> template =
					new BatchTransactionTemplateWithResults<>(requests, requests.size(), Behavior.REPRISE_AUTOMATIQUE, context.transactionManager, null);
			final AcknowledgeTaxDeclarationResults finalReport = new AcknowledgeTaxDeclarationResults();
			template.execute(finalReport, new BatchWithResultsCallback<AcknowledgeTaxDeclarationRequest, AcknowledgeTaxDeclarationResults>() {

				@Override
				public AcknowledgeTaxDeclarationResults createSubRapport() {
					return new AcknowledgeTaxDeclarationResults();
				}

				@Override
				public boolean doInTransaction(List<AcknowledgeTaxDeclarationRequest> batch, AcknowledgeTaxDeclarationResults rapport) throws Exception {
					for (AcknowledgeTaxDeclarationRequest demande : batch) {
						final AcknowledgeTaxDeclarationResponse r = ackDeclaration(demande);
						rapport.addReponse(r);
					}
					return true;
				}
			}, null);
			return finalReport.getReponses();
		}
		catch (RuntimeException e) {
			LOGGER.error(e.getMessage(), e);
			throw ExceptionHelper.newTechnicalException(e);
		}
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public ExtendDeadlineResponse extendDeadline(final ExtendDeadlineRequest request) throws WebServiceException {

		ExtendDeadlineResponse r;
		try {
			r = handleExtendDeadline(request);
		}
		catch (ExtendDeadlineError e) {
			r = ExtendDeadlineBuilder.newExtendDeadlineResponse(request.getKey(), e);
		}
		catch (ValidationException e) {
			LOGGER.error(e.getMessage(), e);
			r = new ExtendDeadlineResponse(request.getKey(), ExtendDeadlineCode.EXCEPTION, new BusinessExceptionInfo(e.getMessage(), BusinessExceptionCode.VALIDATION.name(), null));
		}
		catch (RuntimeException e) {
			LOGGER.error(e.getMessage(), e);
			r = new ExtendDeadlineResponse(request.getKey(), ExtendDeadlineCode.EXCEPTION, new TechnicalExceptionInfo(e.getMessage(), null));
		}
		return r;
	}

	private ExtendDeadlineResponse handleExtendDeadline(ExtendDeadlineRequest request) throws ExtendDeadlineError {

		final TaxDeclarationKey key = request.getKey();

		final ch.vd.uniregctb.tiers.Contribuable ctb = (ch.vd.uniregctb.tiers.Contribuable) context.tiersDAO.get((long) key.getTaxpayerNumber());
		if (ctb == null) {
			throw new ExtendDeadlineError(ExtendDeadlineCode.ERROR_UNKNOWN_TAXPAYER, "Le contribuable est inconnu.");
		}

		final DeclarationImpotOrdinaire declaration = findDeclaration(ctb, key.getTaxPeriod(), key.getSequenceNumber());
		if (declaration == null) {
			throw new ExtendDeadlineError(ExtendDeadlineCode.ERROR_UNKNOWN_TAX_DECLARATION, "La déclaration n'existe pas.");
		}

		if (declaration.isAnnule()) {
			throw new ExtendDeadlineError(ExtendDeadlineCode.ERROR_CANCELLED_TAX_DECLARATION, "La déclaration a été annulée entre-temps.");
		}

		final TypeEtatDeclaration etat = declaration.getDernierEtat().getEtat();
		if (etat != TypeEtatDeclaration.EMISE) {
			throw new ExtendDeadlineError(ExtendDeadlineCode.ERROR_BAD_TAX_DECLARATION_STATUS, "La déclaration n'est pas dans l'état 'émise' (état=[" + etat + "]).");
		}

		final RegDate newDeadline = ch.vd.uniregctb.xml.DataHelper.xmlToCore(request.getNewDeadline());
		final RegDate oldDeadline = declaration.getDernierDelaiAccorde().getDelaiAccordeAu();
		final RegDate today = RegDate.get();

		if (newDeadline.isBefore(today)) {
			throw new ExtendDeadlineError(ExtendDeadlineCode.ERROR_INVALID_DEADLINE,
					"Le délai spécifié [" + RegDateHelper.dateToDisplayString(newDeadline) + "] est antérieur à la date du jour [" + RegDateHelper.dateToDisplayString(today) + "].");
		}
		else if (newDeadline.isBeforeOrEqual(oldDeadline)) {
			throw new ExtendDeadlineError(ExtendDeadlineCode.ERROR_INVALID_DEADLINE,
					"Le délai spécifié [" + RegDateHelper.dateToDisplayString(newDeadline) + "] est antérieur ou égal au délai existant [" + RegDateHelper.dateToDisplayString(oldDeadline) + "].");
		}

		final RegDate applicationDate = ch.vd.uniregctb.xml.DataHelper.xmlToCore(request.getApplicationDate());
		if (applicationDate.isAfter(today)) {
			throw new ExtendDeadlineError(ExtendDeadlineCode.ERROR_INVALID_APPLICATION_DATE,
					"La date de demande spécifiée [" + RegDateHelper.dateToDisplayString(applicationDate) + "] est postérieure à la date du jour [" + RegDateHelper.dateToDisplayString(today) + "].");
		}

		// Le délai est correcte, on l'ajoute
		final DelaiDeclaration delai = new DelaiDeclaration();
		delai.setEtat(EtatDelaiDeclaration.ACCORDE);
		delai.setDateTraitement(RegDate.get());
		delai.setCleArchivageCourrier(null);
		delai.setDateDemande(applicationDate);
		delai.setDelaiAccordeAu(newDeadline);
		declaration.addDelai(delai);

		return ExtendDeadlineBuilder.newExtendDeadlineResponse(key, ExtendDeadlineCode.OK);
	}

	@Override
	public void ping() {
		// rien à faire
	}

	@Override
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public PartyNumberList getModifiedTaxpayers(GetModifiedTaxpayersRequest params) throws WebServiceException {

		try {
			final Date searchBeginDate = XmlUtils.xmlcal2date(params.getSearchBeginDate());
			final Date searchEndDate = XmlUtils.xmlcal2date(params.getSearchEndDate());
			if (DateHelper.isAfter(searchBeginDate, searchEndDate)) {
				throw ExceptionHelper.newBusinessException("La date de début de recherche " + searchBeginDate.toString() + " est après la date de fin " + searchEndDate,
				                                           BusinessExceptionCode.INVALID_REQUEST);
			}
			final List<Long> listCtb = context.tiersDAO.getListeCtbModifies(searchBeginDate, searchEndDate);
			final PartyNumberList list = new PartyNumberList();
			for (Long l : listCtb) {
				// [SIPM] il faut écarter les établissements (les identifiants ne sont pas utilisables avec GetParty/GetParties) et ils étaient de fait écartés auparavant car il n'y en avait pas...
				if (l != null && (l < Etablissement.ETB_GEN_FIRST_ID || l > Etablissement.ETB_GEN_LAST_ID)) {
					list.getItem().add(l.intValue());
				}
			}
			return list;
		}
		catch (RuntimeException e) {
			LOGGER.error(e.getMessage(), e);
			throw ExceptionHelper.newTechnicalException(e);
		}
	}

	private AcknowledgeTaxDeclarationResponse ackDeclaration(AcknowledgeTaxDeclarationRequest demande) {
		AcknowledgeTaxDeclarationResponse r;
		try {
			r = handleRequest(demande);
		}
		catch (TaxDeclarationAcknowledgeError e) {
			r = AcknowledgeTaxDeclarationBuilder.newAcknowledgeTaxDeclarationResponse(demande.getKey(), e);
		}
		catch (ValidationException e) {
			LOGGER.error(e.getMessage(), e);
			r = new AcknowledgeTaxDeclarationResponse(demande.getKey(), TaxDeclarationAcknowledgeCode.EXCEPTION, new BusinessExceptionInfo(e.getMessage(), BusinessExceptionCode.VALIDATION.name(), null));
		}
		catch (RuntimeException e) {
			LOGGER.error(e.getMessage(), e);
			r = new AcknowledgeTaxDeclarationResponse(demande.getKey(), TaxDeclarationAcknowledgeCode.EXCEPTION, new TechnicalExceptionInfo(e.getMessage(), null));
		}
		return r;
	}

	/**
	 * Traite une demande de quittancement de déclaration,
	 *
	 * @param demande la demande de quittancement à traiter
	 * @return la réponse de la demande de quittancement en cas de traitement effectué.
	 * @throws ch.vd.uniregctb.webservices.party3.exception.TaxDeclarationAcknowledgeError
	 *          une erreur explicite en cas d'impossibilité d'effectuer le traitement.
	 */
	private AcknowledgeTaxDeclarationResponse handleRequest(AcknowledgeTaxDeclarationRequest demande) throws TaxDeclarationAcknowledgeError {

		final int number = demande.getKey().getTaxpayerNumber();
		final ch.vd.uniregctb.tiers.Contribuable ctb = (ch.vd.uniregctb.tiers.Contribuable) context.tiersDAO.get((long) number);
		if (ctb == null) {
			throw new TaxDeclarationAcknowledgeError(TaxDeclarationAcknowledgeCode.ERROR_UNKNOWN_TAXPAYER, "Le contribuable est inconnu.");
		}

		if (ctb.getDernierForFiscalPrincipal() == null) {
			throw new TaxDeclarationAcknowledgeError(TaxDeclarationAcknowledgeCode.ERROR_TAX_LIABILITY, "Le contribuable ne possède aucun for principal : il n'aurait pas dû recevoir de déclaration d'impôt.");
		}

		if (ctb.isDebiteurInactif()) {
			throw new TaxDeclarationAcknowledgeError(TaxDeclarationAcknowledgeCode.ERROR_INACTIVE_DEBTOR, "Le contribuable est un débiteur inactif : impossible de quittancer la déclaration.");
		}

		final DeclarationImpotOrdinaire declaration = findDeclaration(ctb, demande.getKey().getTaxPeriod(), demande.getKey().getSequenceNumber());
		if (declaration == null) {
			throw new TaxDeclarationAcknowledgeError(TaxDeclarationAcknowledgeCode.ERROR_UNKNOWN_TAX_DECLARATION, "La déclaration n'existe pas.");
		}

		if (declaration.isAnnule()) {
			throw new TaxDeclarationAcknowledgeError(TaxDeclarationAcknowledgeCode.ERROR_CANCELLED_TAX_DECLARATION, "La déclaration a été annulée entre-temps.");
		}

		final RegDate dateRetour = ch.vd.uniregctb.xml.DataHelper.xmlToCore(demande.getAcknowledgeDate());
		if (RegDateHelper.isBeforeOrEqual(dateRetour, declaration.getDateExpedition(), NullDateBehavior.EARLIEST)) {
			throw new TaxDeclarationAcknowledgeError(TaxDeclarationAcknowledgeCode.ERROR_INVALID_ACKNOWLEDGE_DATE,
					"La date de retour spécifiée (" + dateRetour + ") est avant la date d'envoi de la déclaration (" + declaration.getDateExpedition() + ").");
		}

		// envoie le quittancement au BAM
		sendQuittancementToBam(declaration, dateRetour);

		// La déclaration est correcte, on la quittance
		context.diService.quittancementDI(ctb, declaration, dateRetour, demande.getSource(), true);
		Assert.isEqual(TypeEtatDeclaration.RETOURNEE, declaration.getDernierEtat().getEtat());

		return AcknowledgeTaxDeclarationBuilder.newAcknowledgeTaxDeclarationResponse(demande.getKey(), TaxDeclarationAcknowledgeCode.OK);
	}

	private void sendQuittancementToBam(DeclarationImpotOrdinaire di, RegDate dateQuittancement) {
		final long ctbId = di.getTiers().getNumero();
		final int annee = di.getPeriode().getAnnee();
		final int noSequence = di.getNumero();
		try {
			final Map<String, String> bamHeaders = BamMessageHelper.buildCustomBamHeadersForQuittancementDeclaration(di, dateQuittancement, null);
			final String businessId = String.format("%d-%d-%d-%s", ctbId, annee, noSequence, new SimpleDateFormat("yyyyMMddHHmmssSSS").format(DateHelper.getCurrentDate()));
			final String processDefinitionId = di instanceof DeclarationImpotOrdinairePM ? BamMessageHelper.PROCESS_DEFINITION_ID_PAPIER_PM : BamMessageHelper.PROCESS_DEFINITION_ID_PAPIER_PP;       // pour le moment, tous les quittancements par le WS concenent les DI "papier"
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
	private static DeclarationImpotOrdinaire findDeclaration(final ch.vd.uniregctb.tiers.Contribuable contribuable, final int annee, int numeroSequenceDI) {

		// [SIFISC-1227] Nous avons des cas où le numéro de séquence a été ré-utilisé après annulation d'une DI précédente
		// -> on essaie toujours de renvoyer la déclaration non-annulée qui correspond et, s'il n'y en a pas, de renvoyer
		// la dernière déclaration annulée trouvée

		DeclarationImpotOrdinaire declaration = null;
		DeclarationImpotOrdinaire declarationAnnuleeTrouvee = null;
		final List<DeclarationImpotOrdinaire> declarations = contribuable.getDeclarationsDansPeriode(DeclarationImpotOrdinaire.class, annee, true);
		if (declarations != null && !declarations.isEmpty()) {
			for (DeclarationImpotOrdinaire di : declarations) {
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

}
