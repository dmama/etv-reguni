package ch.vd.uniregctb.webservices.tiers3.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersDAO.Parts;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.webservices.common.NoOfsTranslator;
import ch.vd.uniregctb.webservices.tiers3.BatchTiers;
import ch.vd.uniregctb.webservices.tiers3.CodeQuittancement;
import ch.vd.uniregctb.webservices.tiers3.DebiteurInfo;
import ch.vd.uniregctb.webservices.tiers3.DemandeQuittancementDeclaration;
import ch.vd.uniregctb.webservices.tiers3.GetBatchTiersRequest;
import ch.vd.uniregctb.webservices.tiers3.GetDebiteurInfoRequest;
import ch.vd.uniregctb.webservices.tiers3.GetListeCtbModifiesRequest;
import ch.vd.uniregctb.webservices.tiers3.GetTiersRequest;
import ch.vd.uniregctb.webservices.tiers3.GetTiersTypeRequest;
import ch.vd.uniregctb.webservices.tiers3.QuittancerDeclarationsRequest;
import ch.vd.uniregctb.webservices.tiers3.QuittancerDeclarationsResponse;
import ch.vd.uniregctb.webservices.tiers3.ReponseQuittancementDeclaration;
import ch.vd.uniregctb.webservices.tiers3.SearchEvenementsPMRequest;
import ch.vd.uniregctb.webservices.tiers3.SearchEvenementsPMResponse;
import ch.vd.uniregctb.webservices.tiers3.SearchTiersRequest;
import ch.vd.uniregctb.webservices.tiers3.SearchTiersResponse;
import ch.vd.uniregctb.webservices.tiers3.SetTiersBlocRembAutoRequest;
import ch.vd.uniregctb.webservices.tiers3.Tiers;
import ch.vd.uniregctb.webservices.tiers3.TiersInfo;
import ch.vd.uniregctb.webservices.tiers3.TiersPart;
import ch.vd.uniregctb.webservices.tiers3.TiersWebService;
import ch.vd.uniregctb.webservices.tiers3.TypeTiers;
import ch.vd.uniregctb.webservices.tiers3.TypeWebServiceException;
import ch.vd.uniregctb.webservices.tiers3.WebServiceException;
import ch.vd.uniregctb.webservices.tiers3.data.BatchTiersBuilder;
import ch.vd.uniregctb.webservices.tiers3.data.DebiteurInfoBuilder;
import ch.vd.uniregctb.webservices.tiers3.data.QuittancementBuilder;
import ch.vd.uniregctb.webservices.tiers3.data.TiersBuilder;
import ch.vd.uniregctb.webservices.tiers3.exception.QuittancementErreur;

public class TiersWebServiceImpl implements TiersWebService {

	private static final Logger LOGGER = Logger.getLogger(TiersWebServiceImpl.class);

	private static final int MAX_BATCH_SIZE = 500;
	// la limite Oracle est à 1'000, mais comme on peut recevoir des ménages communs, il faut garder une bonne marge pour charger les personnes physiques associées.

	private final Context context = new Context();

	private GlobalTiersSearcher tiersSearcher;

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

	@Override
	@Transactional(readOnly = true)
	public SearchTiersResponse searchTiers(SearchTiersRequest params) throws WebServiceException {

		try {
			Set<TiersInfo> set = new HashSet<TiersInfo>();

			final List<TiersCriteria> criteria = DataHelper.webToCore(params);
			for (TiersCriteria criterion : criteria) {
				final List<TiersIndexedData> values = tiersSearcher.search(criterion);
				for (TiersIndexedData value : values) {
					final TiersInfo info = DataHelper.coreToWeb(value);
					set.add(info);
				}
			}

			SearchTiersResponse array = new SearchTiersResponse();
			array.getItem().addAll(set);
			return array;
		}
		catch (TooManyResultsIndexerException e) {
			throw ExceptionHelper.newBusinessException(e);
		}
		catch (EmptySearchCriteriaException e) {
			throw ExceptionHelper.newBusinessException(e);
		}
		catch (IndexerException e) {
			LOGGER.error(e, e);
			throw ExceptionHelper.newBusinessException(e);
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw ExceptionHelper.newTechnicalException(e);
		}
	}

	@Override
	public Tiers getTiers(GetTiersRequest params) throws WebServiceException {

		try {
			final ch.vd.uniregctb.tiers.Tiers tiers = context.tiersService.getTiers(params.getTiersNumber());
			if (tiers == null) {
				return null;
			}

			final Tiers data;
			if (tiers instanceof ch.vd.uniregctb.tiers.PersonnePhysique) {
				final ch.vd.uniregctb.tiers.PersonnePhysique personne = (ch.vd.uniregctb.tiers.PersonnePhysique) tiers;
				data = TiersBuilder.newPersonnePhysique(personne, DataHelper.toSet(params.getParts()), context);
			}
			else if (tiers instanceof ch.vd.uniregctb.tiers.MenageCommun) {
				final ch.vd.uniregctb.tiers.MenageCommun menage = (ch.vd.uniregctb.tiers.MenageCommun) tiers;
				data = TiersBuilder.newMenageCommun(menage, DataHelper.toSet(params.getParts()), context);
			}
			else if (tiers instanceof DebiteurPrestationImposable) {
				final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;
				data = TiersBuilder.newDebiteur(debiteur, DataHelper.toSet(params.getParts()), context);
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
	public BatchTiers getBatchTiers(final GetBatchTiersRequest params) throws WebServiceException {
		try {
			if (params.getTiersNumbers() == null || params.getTiersNumbers().isEmpty()) {
				return new BatchTiers();
			}

			if (params.getTiersNumbers().size() > MAX_BATCH_SIZE) {
				final String message = "La taille des requêtes batch ne peut pas dépasser " + MAX_BATCH_SIZE + ".";
				LOGGER.error(message);
				throw ExceptionHelper.newBusinessException(message);
			}

			final Map<Long, Object> results = mapTiers(new HashSet<Long>(params.getTiersNumbers()), null, DataHelper.toSet(params.getParts()), new MapCallback() {
				public Object map(ch.vd.uniregctb.tiers.Tiers tiers, Set<TiersPart> parts, RegDate date, Context context) {
					try {
						final Tiers t;
						if (tiers instanceof ch.vd.uniregctb.tiers.PersonnePhysique) {
							final ch.vd.uniregctb.tiers.PersonnePhysique personne = (ch.vd.uniregctb.tiers.PersonnePhysique) tiers;
							t = TiersBuilder.newPersonnePhysique(personne, parts, context);
						}
						else if (tiers instanceof ch.vd.uniregctb.tiers.MenageCommun) {
							final ch.vd.uniregctb.tiers.MenageCommun menage = (ch.vd.uniregctb.tiers.MenageCommun) tiers;
							t = TiersBuilder.newMenageCommun(menage, parts, context);
						}
						else if (tiers instanceof DebiteurPrestationImposable) {
							final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;
							t = TiersBuilder.newDebiteur(debiteur, parts, context);
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

			return BatchTiersBuilder.newBatchTiers(results);
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw ExceptionHelper.newTechnicalException(e);
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

		// on travaille en utilisant plusieurs threads
		final int nbThreads = Math.max(1, Math.min(5, allIds.size() / 25)); // un thread pour chaque multiple de 25 tiers. Au minimum 1 thread, au maximum 5 threads
		final List<Set<Long>> list = split(allIds, nbThreads);

		// démarrage des threads
		final List<MappingThread> threads = new ArrayList<MappingThread>(nbThreads);
		for (Set<Long> ids : list) {
			MappingThread t = new MappingThread(ids, date, parts, context, callback);
			threads.add(t);
			t.start();
		}

		final Map<Long, Object> results = new HashMap<Long, Object>();

		long loadTiersTime = 0;
		long warmIndividusTime = 0;
		long mapTiersTime = 0;

		// attente de la fin des threads
		for (MappingThread t : threads) {
			try {
				t.join();
			}
			catch (InterruptedException e) {
				// thread interrompu: il ne tourne plus, rien de spécial à faire en fait.
				LOGGER.warn("Le thread " + t.getId() + " a été interrompu", e);
			}
			results.putAll(t.getResults());

			loadTiersTime += t.loadTiersTime;
			warmIndividusTime += t.warmIndividusTime;
			mapTiersTime += t.mapTiersTime;
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
	protected static Set<Parts> webToCoreWithForsFiscaux(Set<TiersPart> parts) {
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
	@Transactional(readOnly = true)
	public TypeTiers getTiersType(GetTiersTypeRequest params) throws WebServiceException {

		try {
			final ch.vd.uniregctb.tiers.Tiers tiers = context.tiersService.getTiers(params.getTiersNumber());
			if (tiers == null) {
				return null;
			}

			final TypeTiers type = DataHelper.getType(tiers);
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
	@Transactional(rollbackFor = Throwable.class)
	public void setTiersBlocRembAuto(final SetTiersBlocRembAutoRequest params) throws WebServiceException {

		try {
			final ch.vd.uniregctb.tiers.Tiers tiers = context.tiersService.getTiers(params.getTiersNumber());
			if (tiers == null) {
				throw ExceptionHelper.newBusinessException("Le tiers n°" + params.getTiersNumber() + " n'existe pas.");
			}

			tiers.setBlocageRemboursementAutomatique(params.isBlocage());
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw ExceptionHelper.newTechnicalException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public SearchEvenementsPMResponse searchEvenementsPM(SearchEvenementsPMRequest params) throws WebServiceException {
		throw ExceptionHelper.newTechnicalException("Fonctionnalité pas encore implémentée.");
	}

	@Transactional(readOnly = true)
	public DebiteurInfo getDebiteurInfo(GetDebiteurInfoRequest params) throws WebServiceException {

		try {
			final ch.vd.uniregctb.tiers.Tiers tiers = context.tiersService.getTiers(params.getNumeroDebiteur());
			if (tiers == null) {
				throw ExceptionHelper.newBusinessException("Le tiers n°" + params.getNumeroDebiteur() + " n'existe pas.");
			}
			if (!(tiers instanceof DebiteurPrestationImposable)) {
				throw ExceptionHelper.newBusinessException("Le tiers n°" + params.getNumeroDebiteur() + " n'est pas un débiteur.");
			}

			final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiers;

			// [UNIREG-2110] Détermine les LRs émises et celles manquantes
			final List<? extends DateRange> lrEmises = debiteur.getDeclarationsForPeriode(params.getPeriodeFiscale());
			final List<DateRange> lrManquantes = context.lrService.findLRsManquantes(debiteur, RegDate.get(params.getPeriodeFiscale(), 12, 31), new ArrayList<DateRange>());

			return DebiteurInfoBuilder.newDebiteurInfo(params, lrEmises, lrManquantes);
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw ExceptionHelper.newTechnicalException(e);
		}
	}

	/**
	 * Classe interne au quittancement des déclarations d'impôt
	 */
	private static class QuittancementResults implements BatchResults<DemandeQuittancementDeclaration, QuittancementResults> {

		private final QuittancerDeclarationsResponse reponses = new QuittancerDeclarationsResponse();

		public void addErrorException(DemandeQuittancementDeclaration element, Exception e) {
			if (e instanceof ValidationException) {
				reponses.getItem().add(QuittancementBuilder.newReponseQuittancementDeclaration(element.getKey(), e, TypeWebServiceException.BUSINESS));
			}
			else if (e instanceof RuntimeException) {
				reponses.getItem().add(QuittancementBuilder.newReponseQuittancementDeclaration(element.getKey(), e, TypeWebServiceException.TECHNICAL));
			}
			else {
				reponses.getItem().add(QuittancementBuilder.newReponseQuittancementDeclaration(element.getKey(), new RuntimeException(e.getMessage(), e), TypeWebServiceException.TECHNICAL));
			}
		}

		public void addAll(QuittancementResults right) {
			this.reponses.getItem().addAll(right.getReponses().getItem());
		}

		public void addReponse(ReponseQuittancementDeclaration reponse) {
			this.reponses.getItem().add(reponse);
		}

		public QuittancerDeclarationsResponse getReponses() {
			return reponses;
		}
	}

	public QuittancerDeclarationsResponse quittancerDeclarations(QuittancerDeclarationsRequest params) throws WebServiceException {

		try {
			final List<DemandeQuittancementDeclaration> demandes = params.getDemandes();
			final BatchTransactionTemplate<DemandeQuittancementDeclaration, QuittancementResults> template =
					new BatchTransactionTemplate<DemandeQuittancementDeclaration, QuittancementResults>(demandes, demandes.size(), BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE,
							context.transactionManager, null, context.hibernateTemplate);
			final QuittancementResults rapportFinal = new QuittancementResults();
			template.execute(rapportFinal, new BatchTransactionTemplate.BatchCallback<DemandeQuittancementDeclaration, QuittancementResults>() {

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
			});
			return rapportFinal.getReponses();
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw ExceptionHelper.newTechnicalException(e);
		}
	}

	@Transactional(readOnly = true)
	public Long[] getListeCtbModifies(GetListeCtbModifiesRequest params) throws WebServiceException {

		try {
			final Date dateDebutRecherche = XmlUtils.xmlcal2date(params.getDateDebutRecherche());
			final Date dateFinRecherche = XmlUtils.xmlcal2date(params.getDateFinRecherche());
			if (DateHelper.isAfter(dateDebutRecherche, dateFinRecherche)) {
				throw ExceptionHelper.newBusinessException("La date de début de recherche " + dateDebutRecherche.toString() + " est après la date de fin " + dateFinRecherche);
			}
			final List<Long> listCtb = context.tiersDAO.getListeCtbModifies(dateDebutRecherche, dateFinRecherche);
			return listCtb.toArray(new Long[listCtb.size()]);
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			throw ExceptionHelper.newTechnicalException(e);
		}
	}

	private ReponseQuittancementDeclaration quittancerDeclaration(DemandeQuittancementDeclaration demande) {
		ReponseQuittancementDeclaration r;
		try {
			r = traiterDemande(demande);
		}
		catch (QuittancementErreur e) {
			r = QuittancementBuilder.newReponseQuittancementDeclaration(demande.getKey(), e);
		}
		catch (ValidationException e) {
			LOGGER.error(e, e);
			r = QuittancementBuilder.newReponseQuittancementDeclaration(demande.getKey(), e, TypeWebServiceException.BUSINESS);
		}
		catch (RuntimeException e) {
			LOGGER.error(e, e);
			r = QuittancementBuilder.newReponseQuittancementDeclaration(demande.getKey(), e, TypeWebServiceException.TECHNICAL);
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

		final ch.vd.uniregctb.tiers.Contribuable ctb = (ch.vd.uniregctb.tiers.Contribuable) context.tiersDAO.get(demande.getKey().getCtbId());
		if (ctb == null) {
			throw new QuittancementErreur(CodeQuittancement.ERREUR_CTB_INCONNU, "Le contribuable est inconnu.");
		}

		if (ctb.getDernierForFiscalPrincipal() == null) {
			throw new QuittancementErreur(CodeQuittancement.ERREUR_ASSUJETTISSEMENT_CTB, "Le contribuable ne possède aucun for principal : il n'aurait pas dû recevoir de déclaration d'impôt.");
		}

		if (ctb.isDebiteurInactif()) {
			throw new QuittancementErreur(CodeQuittancement.ERREUR_CTB_DEBITEUR_INACTIF, "Le contribuable est un débiteur inactif : impossible de quittancer la déclaration.");
		}

		final DeclarationImpotOrdinaire declaration = findDeclaration(ctb, demande.getKey().getPeriodeFiscale(), demande.getKey().getNumeroSequenceDI());
		if (declaration == null) {
			throw new QuittancementErreur(CodeQuittancement.ERREUR_DECLARATION_INEXISTANTE, "La déclaration n'existe pas.");
		}

		if (declaration.isAnnule()) {
			throw new QuittancementErreur(CodeQuittancement.ERREUR_DECLARATION_ANNULEE, "La déclaration a été annulée entre-temps.");
		}

		final RegDate dateRetour = DataHelper.webToCore(demande.getDateRetour());
		if (RegDateHelper.isBeforeOrEqual(dateRetour, declaration.getDateExpedition(), NullDateBehavior.EARLIEST)) {
			throw new QuittancementErreur(CodeQuittancement.ERREUR_DATE_RETOUR_INVALIDE,
					"La date de retour spécifiée (" + dateRetour + ") est avant la date d'envoi de la déclaration (" + declaration.getDateExpedition() + ").");
		}

		// La déclaration est correcte, on la quittance
		context.diService.retourDI(ctb, declaration, dateRetour);
		Assert.isEqual(TypeEtatDeclaration.RETOURNEE, declaration.getDernierEtat().getEtat());

		return QuittancementBuilder.newReponseQuittancementDeclaration(demande.getKey(), CodeQuittancement.OK);
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

		DeclarationImpotOrdinaire declaration = null;

		final List<Declaration> declarations = contribuable.getDeclarationsSorted();
		if (declarations != null && !declarations.isEmpty()) {
			for (Declaration d : declarations) {
				if (d.getPeriode().getAnnee() != annee) {
					continue;
				}
				final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) d;
				if (numeroSequenceDI == 0) {
					// Dans le cas ou le numero dans l'année n'est pas spécifié on prend la dernière DI trouvée sur la période
					declaration = di;
				}
				else {
					if (di.getNumero() == numeroSequenceDI) {
						declaration = di;
						break;
					}
				}

			}
		}

		return declaration;
	}

}
