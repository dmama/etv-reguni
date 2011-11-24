package ch.vd.uniregctb.webservices.party3.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.unireg.webservices.party3.AcknowledgeTaxDeclarationRequest;
import ch.vd.unireg.webservices.party3.AcknowledgeTaxDeclarationResponse;
import ch.vd.unireg.webservices.party3.AcknowledgeTaxDeclarationsRequest;
import ch.vd.unireg.webservices.party3.AcknowledgeTaxDeclarationsResponse;
import ch.vd.unireg.webservices.party3.BatchParty;
import ch.vd.unireg.webservices.party3.GetBatchPartyRequest;
import ch.vd.unireg.webservices.party3.GetDebtorInfoRequest;
import ch.vd.unireg.webservices.party3.GetModifiedTaxpayersRequest;
import ch.vd.unireg.webservices.party3.GetPartyRequest;
import ch.vd.unireg.webservices.party3.GetPartyTypeRequest;
import ch.vd.unireg.webservices.party3.PartyPart;
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
import ch.vd.unireg.xml.party.v1.Party;
import ch.vd.unireg.xml.party.v1.PartyInfo;
import ch.vd.unireg.xml.party.v1.PartyType;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BatchResults;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.source.ListeRecapService;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.indexer.EmptySearchCriteriaException;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.TooManyResultsIndexerException;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.jms.BamMessageHelper;
import ch.vd.uniregctb.jms.BamMessageSender;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersDAO.Parts;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.webservices.party3.data.AcknowledgeTaxDeclarationBuilder;
import ch.vd.uniregctb.webservices.party3.data.BatchPartyBuilder;
import ch.vd.uniregctb.webservices.party3.data.DebtorInfoBuilder;
import ch.vd.uniregctb.webservices.party3.data.PartyBuilder;
import ch.vd.uniregctb.webservices.party3.exception.TaxDeclarationAcknowledgeError;

public class PartyWebServiceImpl implements PartyWebService {

	private static final Logger LOGGER = Logger.getLogger(PartyWebServiceImpl.class);

	private static final int MAX_BATCH_SIZE = 500;
	// la limite Oracle est à 1'000, mais comme on peut recevoir des ménages communs, il faut garder une bonne marge pour charger les personnes physiques associées.

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
	public void setThreadPool(ExecutorService threadPool) {
		this.threadPool = threadPool;
	}

	@Override
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public SearchPartyResponse searchParty(SearchPartyRequest params) throws WebServiceException {

		try {
			Set<PartyInfo> set = new HashSet<PartyInfo>();

			final List<TiersCriteria> criteria = DataHelper.webToCore(params);
			for (TiersCriteria criterion : criteria) {
				final List<TiersIndexedData> values = tiersSearcher.search(criterion);
				for (TiersIndexedData value : values) {
					final PartyInfo info = DataHelper.coreToWeb(value);
					set.add(info);
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
			LOGGER.error(e, e);
			throw ExceptionHelper.newBusinessException(e, BusinessExceptionCode.INDEXER);
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
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
			final Set<PartyPart> parts = DataHelper.toSet(params.getParts());
			if (tiers instanceof ch.vd.uniregctb.tiers.PersonnePhysique) {
				final ch.vd.uniregctb.tiers.PersonnePhysique personne = (ch.vd.uniregctb.tiers.PersonnePhysique) tiers;
				BusinessHelper.warmIndividus(personne, parts, context);
				data = PartyBuilder.newNaturalPerson(personne, parts, context);
			}
			else if (tiers instanceof ch.vd.uniregctb.tiers.MenageCommun) {
				final ch.vd.uniregctb.tiers.MenageCommun menage = (ch.vd.uniregctb.tiers.MenageCommun) tiers;
				BusinessHelper.warmIndividus(menage, parts, context);
				data = PartyBuilder.newCommonHousehold(menage, parts, context);
			}
			else if (tiers instanceof DebiteurPrestationImposable) {
				final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;
				data = PartyBuilder.newDebtor(debiteur, parts, context);
			}
			else {
				data = null;
			}

			return data;
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw ExceptionHelper.newTechnicalException(e);
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
				final String message = "La taille des requêtes batch ne peut pas dépasser " + MAX_BATCH_SIZE + ".";
				LOGGER.error(message);
				throw ExceptionHelper.newBusinessException(message, BusinessExceptionCode.INVALID_REQUEST);
			}

			final Map<Long, Object> results = mapParties(toLongSet(partyNumbers), null, DataHelper.toSet(params.getParts()), new MapCallback() {
				@Override
				public Object map(ch.vd.uniregctb.tiers.Tiers tiers, Set<PartyPart> parts, RegDate date, Context context) {
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
						else if (tiers instanceof DebiteurPrestationImposable) {
							final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;
							t = PartyBuilder.newDebtor(debiteur, parts, context);
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
						return ExceptionHelper.newTechnicalException(e);
					}
				}
			});

			return BatchPartyBuilder.newBatchParty(results);
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw ExceptionHelper.newTechnicalException(e);
		}
	}

	private static HashSet<Long> toLongSet(List<Integer> partyNumbers) {
		final HashSet<Long> numbers = new HashSet<Long>(partyNumbers.size());
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
	private Map<Long, Object> mapParties(Set<Long> partyNumbers, @Nullable RegDate date, Set<PartyPart> parts, MapCallback callback) {

		final Set<Long> allIds = trim(partyNumbers);

		final Map<Long, Object> results = new HashMap<Long, Object>();
		long loadTiersTime = 0;
		long warmIndividusTime = 0;
		long mapTiersTime = 0;

		// on découpe le travail sur plusieurs threads
		final int nbThreads = Math.max(1, Math.min(10, allIds.size() / 10)); // un thread pour chaque multiple de 10 tiers. Au minimum 1 thread, au maximum 10 threads

		if (nbThreads == 1) {
			// un seul thread, on utilise le thread courant
			final MappingThread t = new MappingThread(allIds, date, parts, context, callback);
			t.run();

			results.putAll(t.getResults());
			loadTiersTime += t.loadTiersTime;
			warmIndividusTime += t.warmIndividusTime;
			mapTiersTime += t.mapTiersTime;
		}
		else {
			// plusieurs threads, on délègue au thread pool
			final List<Set<Long>> list = split(allIds, nbThreads);

			// démarrage des threads
			final List<MappingThread> threads = new ArrayList<MappingThread>(nbThreads);
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
				results.putAll(t.getResults());

				loadTiersTime += t.loadTiersTime;
				warmIndividusTime += t.warmIndividusTime;
				mapTiersTime += t.mapTiersTime;
			}
		}

		long totalTime = loadTiersTime + warmIndividusTime + mapTiersTime;

		if (totalTime > 0 && LOGGER.isDebugEnabled()) {
			LOGGER.debug(String.format("temps d'exécution: chargement des tiers=%d%%, préchargement des individus=%d%%, mapping des tiers=%d%%", loadTiersTime * 100 / totalTime,
					warmIndividusTime * 100 / totalTime, mapTiersTime * 100 / totalTime));
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

		List<Set<Long>> list = new ArrayList<Set<Long>>();

		for (int i = 0; i < n; i++) {
			Set<Long> ids = new HashSet<Long>();
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
			HashSet<Long> trimmed = new HashSet<Long>(input);
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
	protected static Set<Parts> webToCoreWithForsFiscaux(Set<PartyPart> parts) {
		Set<Parts> coreParts = DataHelper.webToCore(parts);
		if (coreParts == null) {
			coreParts = new HashSet<Parts>();
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
				Assert.fail("TypeTiers de tiers inconnu = [" + tiers.getClass().getSimpleName());
			}

			return type;
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
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
			if (tiers == null) {
				throw ExceptionHelper.newBusinessException("Le tiers n°" + params.getPartyNumber() + " n'existe pas.", BusinessExceptionCode.UNKNOWN_PARTY);
			}

			tiers.setBlocageRemboursementAutomatique(params.isBlocked());
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw ExceptionHelper.newTechnicalException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SearchCorporationEventsResponse searchCorporationEvents(SearchCorporationEventsRequest params) throws WebServiceException {
		throw ExceptionHelper.newTechnicalException("Fonctionnalité pas encore implémentée.");
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
			final List<? extends DateRange> lrEmises = debiteur.getDeclarationsForPeriode(params.getTaxPeriod(), false);
			final List<DateRange> lrManquantes = context.lrService.findLRsManquantes(debiteur, RegDate.get(params.getTaxPeriod(), 12, 31), new ArrayList<DateRange>());

			return DebtorInfoBuilder.newDebtorInfo(params, lrEmises, lrManquantes);
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
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
			final BatchTransactionTemplate<AcknowledgeTaxDeclarationRequest, AcknowledgeTaxDeclarationResults> template =
					new BatchTransactionTemplate<AcknowledgeTaxDeclarationRequest, AcknowledgeTaxDeclarationResults>(requests, requests.size(), BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE,
							context.transactionManager, null, context.hibernateTemplate);
			final AcknowledgeTaxDeclarationResults finalReport = new AcknowledgeTaxDeclarationResults();
			template.execute(finalReport, new BatchTransactionTemplate.BatchCallback<AcknowledgeTaxDeclarationRequest, AcknowledgeTaxDeclarationResults>() {

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
			});
			return finalReport.getReponses();
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw ExceptionHelper.newTechnicalException(e);
		}
	}

	@Override
	public void ping() {
		// rien à faire
	}

	@Override
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public Integer[] getModifiedTaxpayers(GetModifiedTaxpayersRequest params) throws WebServiceException {

		try {
			final Date searchBeginDate = XmlUtils.xmlcal2date(params.getSearchBeginDate());
			final Date searchEndDate = XmlUtils.xmlcal2date(params.getSearchEndDate());
			if (DateHelper.isAfter(searchBeginDate, searchEndDate)) {
				throw ExceptionHelper.newBusinessException("La date de début de recherche " + searchBeginDate.toString() + " est après la date de fin " + searchEndDate,
						BusinessExceptionCode.INVALID_REQUEST);
			}
			final List<Long> listCtb = context.tiersDAO.getListeCtbModifies(searchBeginDate, searchEndDate);
			final List<Integer> list = new ArrayList<Integer>(listCtb.size());
			for (Long l : listCtb) {
				list.add(l.intValue());
			}
			return list.toArray(new Integer[list.size()]);
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
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
			LOGGER.error(e, e);
			r = new AcknowledgeTaxDeclarationResponse(demande.getKey(), TaxDeclarationAcknowledgeCode.EXCEPTION, new BusinessExceptionInfo(e.getMessage(), BusinessExceptionCode.VALIDATION.name(), null));
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
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

		final RegDate dateRetour = DataHelper.webToCore(demande.getAcknowledgeDate());
		if (RegDateHelper.isBeforeOrEqual(dateRetour, declaration.getDateExpedition(), NullDateBehavior.EARLIEST)) {
			throw new TaxDeclarationAcknowledgeError(TaxDeclarationAcknowledgeCode.ERROR_INVALID_ACKNOWLEDGE_DATE,
					"La date de retour spécifiée (" + dateRetour + ") est avant la date d'envoi de la déclaration (" + declaration.getDateExpedition() + ").");
		}

		// envoie le quittancement au BAM
		sendQuittancementToBam(declaration, dateRetour);

		// La déclaration est correcte, on la quittance
		context.diService.quittancementDI(ctb, declaration, dateRetour, demande.getSource());
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
	private static DeclarationImpotOrdinaire findDeclaration(final ch.vd.uniregctb.tiers.Contribuable contribuable, final int annee, int numeroSequenceDI) {

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

}
