package ch.vd.uniregctb.identification.contribuable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.securite.model.Operateur;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.DefaultThreadFactory;
import ch.vd.uniregctb.common.DefaultThreadNameGenerator;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresAdresse;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresPersonne;
import ch.vd.uniregctb.evenement.identification.contribuable.Demande;
import ch.vd.uniregctb.evenement.identification.contribuable.DemandeHandler;
import ch.vd.uniregctb.evenement.identification.contribuable.Erreur;
import ch.vd.uniregctb.evenement.identification.contribuable.Erreur.TypeErreur;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentCtbDAO;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableCriteria;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableEtatFilter;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableMessageHandler;
import ch.vd.uniregctb.evenement.identification.contribuable.Reponse;
import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.TooManyResultsIndexerException;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;

public class IdentificationContribuableServiceImpl implements IdentificationContribuableService, DemandeHandler, InitializingBean, DisposableBean {

	private static final Logger LOGGER = Logger.getLogger(IdentificationContribuableServiceImpl.class);

	private static final String REPARTITION_INTERCANTONALE = "ssk-3001-000101";

	private GlobalTiersSearcher searcher;
	private TiersDAO tiersDAO;
	private IdentCtbDAO identCtbDAO;
	private TiersService tiersService;
	private AdresseService adresseService;
	private ServiceInfrastructureService infraService;
	private IdentificationContribuableMessageHandler messageHandler;
	private PlatformTransactionManager transactionManager;
	private ServiceSecuriteService serviceSecuriteService;
	private IdentificationContribuableHelper identificationContribuableHelper;
	private HibernateTemplate hibernateTemplate;
	private int flowSearchThreadPoolSize;

	private IdentificationContribuableCache identificationContribuableCache = new IdentificationContribuableCache();        // cache vide à l'initialisation
	private ExecutorService asynchronousFlowSearchExecutor;

	public void setIdentificationContribuableHelper(IdentificationContribuableHelper identificationContribuableHelper) {
		this.identificationContribuableHelper = identificationContribuableHelper;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setSearcher(GlobalTiersSearcher searcher) {
		this.searcher = searcher;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setIdentCtbDAO(IdentCtbDAO identCtbDAO) {
		this.identCtbDAO = identCtbDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setMessageHandler(IdentificationContribuableMessageHandler handler) {
		this.messageHandler = handler;
	}

	public void setServiceSecuriteService(ServiceSecuriteService serviceSecuriteService) {
		this.serviceSecuriteService = serviceSecuriteService;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setFlowSearchThreadPoolSize(int flowSearchThreadPoolSize) {
		this.flowSearchThreadPoolSize = flowSearchThreadPoolSize;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (flowSearchThreadPoolSize < 1) {
			throw new IllegalArgumentException("Invalid value for the flowSearchThreadPoolSize property : " + flowSearchThreadPoolSize);
		}

		if (isUpdateCriteresOnStartup()) {
			// en fonction du nombre de demandes d'identification en base, le chargement des valeurs possibles des critères de
			// recherche peut prendre quelques secondes (minutes ?), donc on met ça dans un thread séparés afin de ne pas
			// poser de problème de lenteur exagérée au démarrage de l'application (surtout pour l'équipe de développement
			// qui peut avoir à démarrer l'application souvent...)
			final Thread thread = new Thread() {
				@Override
				public void run() {
					LOGGER.info("Préchargement des valeurs pour les critères de recherche pour l'identification des contribuables");
					updateCriteres();
					LOGGER.info("Préchargement terminé.");
				}
			};
			thread.start();
		}

		asynchronousFlowSearchExecutor = Executors.newFixedThreadPool(flowSearchThreadPoolSize, new DefaultThreadFactory(new DefaultThreadNameGenerator("Identification")));
	}

	protected boolean isUpdateCriteresOnStartup() {
		return true;
	}

	@Override
	public void destroy() throws Exception {
		asynchronousFlowSearchExecutor.shutdown();
	}

	private static enum PhaseRechercheSurNomPrenom {

		/**
		 * Première phase de recherche sur le nom et le prénom
		 */
		STANDARD,

		/**
		 * PhaseRechercheContribuable de recherche en enlevant le dernier nom
		 */
		SANS_DERNIER_NOM,

		/**
		 * phase de recherche en enlevant le dernier prenom
		 */

		SANS_DERNIER_PRENOM,

		/**
		 * On enleve les "e"
		 */
		STANDARD_SANS_E,

		/**
		 * On enleve les "e" et le dernier nom
		 */
		SANS_DERNIER_NOM_SANS_E,


		/**
		 * On enleve les "e" et le dernier prénom
		 */
		SANS_DERNIER_PRENOM_SANS_E
	}

	private static interface IdFetcher<T> {
		Long getId(T element);
	}

	private static <T> List<Long> buildIdList(List<T> src, IdFetcher<T> idFetcher) {
		if (src == null) {
			return Collections.emptyList();
		}

		final List<Long> res = new ArrayList<>(src.size());
		for (T elt : src) {
			res.add(idFetcher.getId(elt));
		}
		return res;
	}

	private static List<Long> buildIdListFromPP(List<PersonnePhysique> list) {
		return buildIdList(list, new IdFetcher<PersonnePhysique>() {
			@Override
			public Long getId(PersonnePhysique pp) {
				return pp.getNumero();
			}
		});
	}

	private static List<Long> buildIdListFromIndex(List<TiersIndexedData> list) {
		return buildIdList(list, new IdFetcher<TiersIndexedData>() {
			@Override
			public Long getId(TiersIndexedData data) {
				return data.getNumero();
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Long> identifie(CriteresPersonne criteres) throws TooManyIdentificationPossibilitiesException {

		// 1. phase AVS13
		final List<TiersIndexedData> indexedAvs13 = findByNavs13(criteres);
		if (indexedAvs13 != null && !indexedAvs13.isEmpty()) {
			final List<PersonnePhysique> ppList = getListePersonneFromIndexedData(indexedAvs13);
			filterCoherenceAfterIdentificationAvs13(ppList, criteres);
			if (isIdentificationOK(ppList)) {
				return buildIdListFromPP(ppList);
			}
		}

		// 2. si rien trouvé d'unique, on passe à la phase noms/prénoms...
		final List<TiersIndexedData> indexedComplets = findAvecCriteresComplets(criteres, NB_MAX_RESULTS_POUR_LISTE_IDENTIFICATION);
		return buildIdListFromIndex(indexedComplets);
	}

	private List<PersonnePhysique> filterCoherenceAfterIdentificationAvs13(List<PersonnePhysique> list, CriteresPersonne criteres) {
		//Contrôle des résultats avec le sexe et la date de naissance
		filterSexe(list, criteres);
		filterDateNaissance(list, criteres);

		if (isIdentificationOK(list)) {
			return list;
		}
		else {
			//Controle des résultats avec le nom et le prénom
			filterNomPrenom(list, criteres);
			if (isIdentificationOK(list)) {
				return list;
			}
			else {
				return Collections.emptyList();
			}
		}
	}

	/**
	 * Permet de qualifier une tentative d'identification à partir du contenu d'une liste de personne return vrai si la liste contient une seul entrée, faux sinon.
	 */
	private static boolean isIdentificationOK(List<?> candidates) {
		return (candidates != null && candidates.size() == 1);
	}

	private List<PersonnePhysique> getListePersonneFromIndexedData(List<TiersIndexedData> listIndexedData) {
		final List<PersonnePhysique> list = new ArrayList<>();
		for (TiersIndexedData d : listIndexedData) {
			final Tiers t = tiersDAO.get(d.getNumero());
			if (t != null && t instanceof PersonnePhysique) {
				list.add((PersonnePhysique) t);
			}
		}
		return list;
	}

	public List<TiersIndexedData> findByNavs13(CriteresPersonne criteres) {
		final TiersCriteria criteria = asTiersCriteriaNAVS13(criteres);
		if (!criteria.isEmpty()) {
			try {
				return searcher.search(criteria);
			}
			catch (IndexerException e) {
				if (e instanceof TooManyResultsIndexerException) {
					return Collections.emptyList();
				}
				else {
					throw new RuntimeException(e);
				}
			}
		}
		return Collections.emptyList();
	}

	private List<TiersIndexedData> findAvecCriteresComplets(CriteresPersonne criteres, int maxNumberForList) throws TooManyIdentificationPossibilitiesException {

		List<TiersIndexedData> indexedData = null;

		// [SIFISC-147] effectue la recherche sur les nom et prénoms en plusieurs phase
		for (PhaseRechercheSurNomPrenom phase : PhaseRechercheSurNomPrenom.values()) {
			final TiersCriteria criteria = asTiersCriteriaForNomPrenom(criteres, phase);
			if (criteres.getNAVS11() != null) {
				// on ne compare que les 8 premiers caractères
				criteria.setNavs11OrNull(StringUtils.left(criteres.getNAVS11(), 8));
			}
			if (criteres.getSexe() != null) {
				criteria.setSexeOrNull(criteres.getSexe());
			}
			if (criteres.getDateNaissance() != null) {
				criteria.setDateNaissanceOrNull(criteres.getDateNaissance());
			}
			if (criteres.getAdresse() != null) {
				final CriteresAdresse adresse = criteres.getAdresse();
				final String npa = adresse.getNpaSuisse() == null ? StringUtils.trimToNull(adresse.getNpaEtranger()) : Integer.toString(adresse.getNpaSuisse());
				criteria.setNpaTousOrNull(npa);
			}
			if (criteres.getNAVS13() != null) {
				criteria.setNavs13OrNull(criteres.getNAVS13());
			}

			if (!criteria.isEmpty()) {
				indexedData = searcher.searchTop(criteria, maxNumberForList + 1);
				if (indexedData != null && !indexedData.isEmpty()) {
					if (indexedData.size() > maxNumberForList) {
						throw new TooManyIdentificationPossibilitiesException(maxNumberForList);
					}
					break;
				}
			}
		}

		if (indexedData == null || indexedData.isEmpty()) {
			return Collections.emptyList();
		}
		return indexedData;
	}


	/**
	 * Sauve la demande en base, identifie le ou les contribuables et retourne une réponse immédiatement si un seul contribuable est trouvé. Dans tous les autres cas (0, >1 ou en cas d'erreur), la
	 * demande est stockée pour traitement manuel.
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void handleDemande(IdentificationContribuable message) {

		// Première chose à faire : sauver le message pour allouer un id technique.
		message.setEtat(Etat.RECU);
		message = identCtbDAO.save(message);
		soumettre(message);

	}

	/**
	 * Envoie une réponse d'identification <b>lorsqu'un contribuable a été identifié formellement</b>.
	 *
	 * @param message  la requête d'identification initiale
	 * @param personne la personne physique identifiée
	 * @param etat     le mode d'identification (manuel ou automatique)
	 * @throws Exception si ça a pas marché
	 */
	private void identifie(IdentificationContribuable message, PersonnePhysique personne, Etat etat) throws Exception {
		Assert.notNull(personne);

		// [UNIREG-1911] On retourne le numéro du ménage-commun associé s'il existe
		// [SIFISC-1725] La date de référence pour le couple doit être en rapport avec la période fiscale du message
		//[SIFISC-4845] Calcul de la période fiscale
		//Dans le cas ou la période fiscale du message est inférieure à 2003,
		//Unireg renvoi le contribuable à la date du 31.12 de l'année précédent à l'année en cours (ex. au 21 janvier 2013,
		// le traitement d'une PF de 2001 donnera le contribuable à la date du 31.12.2012).
		//Dans le cas où la période fiscale se situe dans le futur,
		//Unireg renvoi le contribuable à la date courante (ex. au 21 janvier 2013,
		//Le traitement d'une PF de 2211 donnera le contribuable à la date du 21.01.2013).

		RegDate dateReferenceMenage;
		final int periodeFiscale = message.getDemande().getPeriodeFiscale();
		final int anneeCourante = RegDate.get().year();
		if (periodeFiscale < 2003) {
			dateReferenceMenage = RegDate.get(anneeCourante -1, 12, 31);
		}
		else if (periodeFiscale >=  anneeCourante) {
			dateReferenceMenage = RegDate.get();
		}
		else{
			dateReferenceMenage = RegDate.get(periodeFiscale, 12, 31);
		}

		Long mcId = null;
		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(personne, dateReferenceMenage);
		if (ensemble != null) {
			mcId = ensemble.getMenage().getNumero();
		}
		// [UNIREG-1940] On met à jour le contribuable si
		// - le contribuable trouvé est « non habitant »
		// - le message sur lequel a porté l’identification est une « répartition intercantonale »
		// - le NPA de l’adresse contenue dans le message est un NPA du canton d’où provient le message
		verifierEtMettreAJourContribuable(message, personne);

		final String user = AuthenticationHelper.getCurrentPrincipal();

		final Reponse reponse = new Reponse();
		reponse.setDate(DateHelper.getCurrentDate());
		reponse.setNoContribuable(personne.getNumero());
		reponse.setNoMenageCommun(mcId);

		final IdentificationContribuable messageReponse = new IdentificationContribuable();
		messageReponse.setId(message.getId());
		messageReponse.setHeader(message.getHeader());
		messageReponse.setNbContribuablesTrouves(1);
		messageReponse.setReponse(reponse);
		messageReponse.setEtat(etat);
		messageReponse.getHeader().setBusinessUser(traduireBusinessUser(user));

		message.setNbContribuablesTrouves(1);
		message.setReponse(reponse);
		message.setEtat(etat);
		message.setDateTraitement(DateHelper.getCurrentDate());
		message.setTraitementUser(user);

		LOGGER.info("Le message n°" + messageReponse.getId() + " est passé dans l'état [" + etat
				+ "]. Numéro du contribuable trouvé = " + personne.getNumero());

		messageHandler.sendReponse(messageReponse);
	}

	/**
	 * Verifie et met a jour le contribuable avec les données contenus dans le message
	 *
	 * @param message
	 * @param personne
	 * @throws ServiceInfrastructureException
	 */

	private void verifierEtMettreAJourContribuable(IdentificationContribuable message, PersonnePhysique personne)
			throws ServiceInfrastructureException {
		if (!personne.isHabitantVD() && REPARTITION_INTERCANTONALE.equals(message.getDemande().getTypeMessage())
				&& isAdresseFromCantonEmetteur(message)) {
			CriteresPersonne criteres = message.getDemande().getPersonne();

			if (criteres.getNAVS13() != null) {
				personne.setNumeroAssureSocial(criteres.getNAVS13());
			}
		}

	}

	private void setUpAdresse(IdentificationContribuable message, CriteresAdresse criteresAdresse, Integer onrp,
	                          AdresseSuisse adresseCourrier) {
		adresseCourrier.setUsage(TypeAdresseTiers.COURRIER);
		adresseCourrier.setDateDebut(RegDateHelper.get(message.getLogCreationDate()));
		String complement = null;
		if (criteresAdresse.getLigneAdresse1() != null) {
			complement = criteresAdresse.getLigneAdresse1();

		}
		if (criteresAdresse.getLigneAdresse2() != null) {
			complement = complement + ' ' + criteresAdresse.getLigneAdresse2();

		}
		adresseCourrier.setComplement(complement);
		adresseCourrier.setRue(criteresAdresse.getRue());
		adresseCourrier.setNumeroAppartement(criteresAdresse.getNoAppartement());
		adresseCourrier.setNumeroMaison(criteresAdresse.getNoPolice());
		adresseCourrier.setNumeroOrdrePoste(onrp);
		adresseCourrier.setNumeroCasePostale(criteresAdresse.getNumeroCasePostale());
	}

	/**
	 * Permet de savoir si le NPA de l’adresse contenue dans le message est un NPA du canton d’où provient le message
	 *
	 * @param message
	 * @return
	 * @throws ServiceInfrastructureException
	 */
	protected boolean isAdresseFromCantonEmetteur(IdentificationContribuable message) throws ServiceInfrastructureException {
		final CriteresAdresse adresse = message.getDemande().getPersonne().getAdresse();
		if (adresse != null) {
			final String emetteur = message.getDemande().getEmetteurId();
			final String sigle = getSigleCantonEmetteur(emetteur);
			final Integer npa = adresse.getNpaSuisse();
			if (npa != null && sigle != null) {
				final Localite localite = infraService.getLocaliteByNPA(npa);
				if (localite != null) {
					final Commune commune = localite.getCommuneLocalite();
					return commune != null && sigle.equals(commune.getSigleCanton());
				}
			}
		}
		return false;
	}

	protected static String getSigleCantonEmetteur(String emetteurId) {
		final Pattern pattern = Pattern.compile("[0-9]-([A-Z]{2})-[0-9]");
		final Matcher matcher = pattern.matcher(emetteurId == null ? StringUtils.EMPTY : emetteurId);
		if (matcher.matches()) {
			return matcher.group(1);
		}
		else {
			return null;
		}
	}

	/**
	 * Envoie une réponse <b>lorsqu'un contribuable n'a définitivement pas été identifié</b>.
	 *
	 * @param message la requête d'identification initiale
	 * @throws Exception si ça a pas marché
	 */
	private void nonIdentifie(IdentificationContribuable message, Erreur erreur) throws Exception {

		final Etat etat = Etat.NON_IDENTIFIE; // par définition

		final String user = AuthenticationHelper.getCurrentPrincipal();
		final Reponse reponse = new Reponse();
		reponse.setDate(DateHelper.getCurrentDate());
		reponse.setErreur(erreur);
		final IdentificationContribuable messageReponse = new IdentificationContribuable();
		messageReponse.setId(message.getId());
		messageReponse.setHeader(message.getHeader());
		messageReponse.setNbContribuablesTrouves(0);
		messageReponse.setReponse(reponse);
		messageReponse.setEtat(etat);
		messageReponse.getHeader().setBusinessUser(traduireBusinessUser(user));

		message.setNbContribuablesTrouves(0);
		message.setReponse(reponse);
		message.setEtat(etat);
		message.setDateTraitement(DateHelper.getCurrentDate());
		message.setTraitementUser(user);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Le message n°" + messageReponse.getId() + " est passé dans l'état [" + etat + "]. Aucun contribuable trouvé.");
		}

		messageHandler.sendReponse(messageReponse);
	}

	/**
	 * Envoie une réponse <b>Permettant de notifier que le message est en attente d'une identification manuelle</b>
	 *
	 * @param message
	 */
	private void notifieAttenteIdentifManuel(IdentificationContribuable message) throws Exception {
		final Etat etat = Etat.A_TRAITER_MANUELLEMENT; // par définition

		Reponse reponse = new Reponse();
		reponse.setDate(DateHelper.getCurrentDate());
		reponse.setEnAttenteIdentifManuel(true);
		IdentificationContribuable messageReponse = new IdentificationContribuable();
		messageReponse.setId(message.getId());
		messageReponse.setHeader(message.getHeader());
		messageReponse.setReponse(reponse);
		messageReponse.setEtat(etat);

		message.setNbContribuablesTrouves(0);
		message.setReponse(reponse);
		message.setEtat(etat);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Le message n°" + messageReponse.getId() + " est passé dans l'état [" + etat + "], le demandeur va en être notifié.");
		}

		messageHandler.sendReponse(messageReponse);
	}

	/**
	 * Recherche une liste d'IdentificationContribuable en fonction de critères
	 *
	 *
	 *
	 * @param identificationContribuableCriteria
	 *
	 * @param paramPagination
	 * @param filter
	 *@param typeDemande  @return
	 */
	@Override
	public List<IdentificationContribuable> find(IdentificationContribuableCriteria identificationContribuableCriteria,
	                                             ParamPagination paramPagination,
	                                             IdentificationContribuableEtatFilter filter, TypeDemande... typeDemande) {
		return identCtbDAO.find(identificationContribuableCriteria, paramPagination, filter, typeDemande);
	}

	/**
	 * Nombre d'IdentificationContribuable en fonction de critères
	 *
	 *
	 *
	 * @param identificationContribuableCriteria
	 *
	 * @param filter
	 *@param typeDemande  @return
	 */
	@Override
	public int count(IdentificationContribuableCriteria identificationContribuableCriteria,
	                 IdentificationContribuableEtatFilter filter, TypeDemande... typeDemande) {
		return identCtbDAO.count(identificationContribuableCriteria, filter, typeDemande);
	}

	/**
	 * Force l'identification du contribuable
	 *
	 * @param identificationContribuable
	 * @param personne
	 * @throws Exception
	 */
	@Override
	public void forceIdentification(IdentificationContribuable identificationContribuable, PersonnePhysique personne, Etat etat)
			throws Exception {

		identificationContribuable.setEtat(etat); // <--- ça sert à quoi de le faire ici ?
		identifie(identificationContribuable, personne, etat);
	}

	/**
	 * Soumet le message à l'identification
	 *
	 * @param message
	 */
	@Override
	public void soumettre(IdentificationContribuable message) {
		final Demande demande = message.getDemande();
		Assert.notNull(demande, "Le message ne contient aucune demande.");
		final TypeDemande typeDemande = demande.getTypeDemande();
		switch (typeDemande) {
		case MELDEWESEN:
			soumettreMessageMeldewesen(message);
			break;
		case NCS:
			soumettreMessageNCS(message);
			break;
		case E_FACTURE:
			soumettreMessageEfacture(message);
			break;

		case IMPOT_SOURCE:
			soumettreMessageImpotSource(message);
			break;
		default:
			traiterException(message, new IllegalArgumentException("Type de demande inconnue"));
		}
	}

	private void soumettreMessageNCS(IdentificationContribuable message) {
		LOGGER.info("Le message n°" + message.getId() + " passe en traitement NCS.");
		soumettreMessage(message);
	}

	private void soumettreMessageMeldewesen(IdentificationContribuable message) {
		LOGGER.info("Le message n°" + message.getId() + " passe en traitement Meldewesen.");
		soumettreMessage(message);
	}

	private void soumettreMessageEfacture(IdentificationContribuable message) {
		LOGGER.info("Le message n°" + message.getId() + " passe en traitement E_FACTURE.");
		soumettreMessage(message);
	}


	private void soumettreMessageImpotSource(IdentificationContribuable message) {
		LOGGER.info("Le message n°" + message.getId() + " passe en traitement Impot source.");
		soumettreMessage(message);
	}

	//Methode à spécialiser pour les differentes types de demande dans l'avenir. Pour l'instant elle est
	// utilisée pour traiter tout type de messages
	private void soumettreMessage(IdentificationContribuable message) {
		// Ensuite : effectuer l'identification
		try {
			final Demande demande = message.getDemande();
			Assert.notNull(demande, "Le message ne contient aucune demande.");

			final CriteresPersonne criteresPersonne = demande.getPersonne();
			Assert.notNull(demande, "Le message ne contient aucun critère sur la personne à identifier.");

			Integer foundSize;
			List<Long> found;
			try {
				found = identifie(criteresPersonne);
				foundSize = found.size();
			}
			catch (TooManyIdentificationPossibilitiesException e) {
				found = Collections.emptyList();
				foundSize = null;
			}
			if (found.size() == 1) {
				// on a trouvé un et un seul contribuable:
				final PersonnePhysique personne = (PersonnePhysique) tiersDAO.get(found.get(0));

				// on peut répondre immédiatement
				identifie(message, personne, Etat.TRAITE_AUTOMATIQUEMENT);
			}
			else {
				//UNIREG 2412 Ajout de possibilités au service d'identification UniReg asynchrone
				if (Demande.ModeIdentificationType.SANS_MANUEL == demande.getModeIdentification()) {
					final String contenuMessage = "Aucun contribuable n’a été trouvé avec l’identification automatique et l’identification manuelle a été exclue.";
					final Erreur erreur = new Erreur(TypeErreur.METIER, IdentificationContribuable.ErreurMessage.AUCUNE_CORRESPONDANCE.getCode(), contenuMessage);
					nonIdentifie(message, erreur);
				}
				else {
					if (Demande.ModeIdentificationType.MANUEL_AVEC_ACK == demande.getModeIdentification()) {
						notifieAttenteIdentifManuel(message);
					}

					// dans le cas MANUEL_AVEC_ACK et MANUEL_SANS_ACK le message est mis en traitement manuel
					message.setNbContribuablesTrouves(foundSize);
					message.setEtat(Etat.A_TRAITER_MANUELLEMENT);

					if (LOGGER.isDebugEnabled()) {
						final StringBuilder b = new StringBuilder();
						b.append("Le message ").append(message.getId()).append(" doit être traité manuellement. ");
						if (foundSize != null) {
							if (foundSize == 0) {
								b.append("Aucun contribuable trouvé.");
							}
							else {
								b.append(foundSize).append(" contribuables trouvés : ").append(ArrayUtils.toString(found.toArray(new Long[foundSize]))).append('.');
							}
						}
						else {
							b.append("Plus de ").append(NB_MAX_RESULTS_POUR_LISTE_IDENTIFICATION).append(" contribuables trouvés.");
						}
						LOGGER.debug(b.toString());
					}
				}
			}
		}
		catch (Exception e) {
			traiterException(message, e);
		}
	}

	private void traiterException(IdentificationContribuable message, Exception e) {
		LOGGER.warn("Exception lors du traitement du message n°" + message.getId() + ". Le message sera traité manuellement.", e);


		final Demande demande = message.getDemande();
		// toute exception aura pour conséquence de provoquer un traitement manuel: on n'envoie donc pas de réponse immédiatement,sauf en cas de demande d'accusé de reception
		if (demande != null && Demande.ModeIdentificationType.MANUEL_AVEC_ACK == demande.getModeIdentification()) {
			try {
				notifieAttenteIdentifManuel(message);
				//Le message contient la réponse de notification, on lui rajoute l'erreur
				message.getReponse().setErreur(new Erreur(TypeErreur.TECHNIQUE, null, e.getMessage()));
			}
			catch (Exception ex) {
				LOGGER.warn("Exception lors de l'envoi de l'accusée de reception du message n°" + message.getId() + ". Le message sera traité manuellement.", ex);
			}

		}
		else {

			// on stocke le message d'erreur dans le champs reponse.erreur.message par commodité dans le cas ou aucun accusé de reception est demandé

			Reponse reponse = new Reponse();
			reponse.setErreur(new Erreur(TypeErreur.TECHNIQUE, null, e.getMessage()));

			message.setNbContribuablesTrouves(null);
			message.setReponse(reponse);

		}
		message.setEtat(Etat.EXCEPTION);
	}


	/**
	 * Impossible à identifier
	 *
	 * @param identificationContribuable
	 * @throws Exception
	 */
	@Override
	public void impossibleAIdentifier(IdentificationContribuable identificationContribuable, Erreur erreur) throws Exception {
		nonIdentifie(identificationContribuable, erreur);
	}

	private TiersCriteria asTiersCriteriaNAVS13(CriteresPersonne criteres) {
		final TiersCriteria criteria = new TiersCriteria();
		identificationContribuableHelper.setUpCriteria(criteria);
		final String navs13 = criteres.getNAVS13();
		criteria.setNumeroAVS(navs13);
		return criteria;
	}

	private TiersCriteria asTiersCriteriaForNomPrenom(CriteresPersonne criteres, PhaseRechercheSurNomPrenom phase) {

		final TiersCriteria criteria = new TiersCriteria();
		switch (phase) {
		case STANDARD:
			identificationContribuableHelper.updateCriteriaStandard(criteres, criteria);
			break;
		case SANS_DERNIER_NOM:
			identificationContribuableHelper.updateCriteriaSansDernierNom(criteres, criteria);
			break;
		case SANS_DERNIER_PRENOM:
			identificationContribuableHelper.updateCriteriaSansDernierPrenom(criteres, criteria);
			break;
		case STANDARD_SANS_E:
			identificationContribuableHelper.updateCriteriaStandardSansE(criteres, criteria);
			break;
		case SANS_DERNIER_NOM_SANS_E:
			identificationContribuableHelper.updateCriteriaSansDernierNomSansE(criteres, criteria);
			break;
		case SANS_DERNIER_PRENOM_SANS_E:
			identificationContribuableHelper.updateCriteriaSansDernierPrenomSansE(criteres, criteria);
			break;

		}
		return criteria;
	}


	private List<PersonnePhysique> filterNomPrenom(List<PersonnePhysique> list, CriteresPersonne criteres) {
		final String nomCritere = criteres.getNom();
		final String prenomCritere = criteres.getPrenoms();
		if (nomCritere != null) {
			CollectionUtils.filter(list, new Predicate() {
				@Override
				public boolean evaluate(Object object) {
					final PersonnePhysique pp = (PersonnePhysique) object;
					final String nomPrenom = tiersService.getNomPrenom(pp);
					return (nomPrenom.contains(nomCritere));
				}
			});
		}

		if (prenomCritere != null) {
			CollectionUtils.filter(list, new Predicate() {
				@Override
				public boolean evaluate(Object object) {
					final PersonnePhysique pp = (PersonnePhysique) object;
					final String nomPrenom = tiersService.getNomPrenom(pp);
					return (nomPrenom.contains(prenomCritere));
				}
			});
		}

		return list;
	}

	/**
	 * Supprime toutes les personnes de sexe différent de celui spécifié
	 *
	 * @param list     la liste des personnes à fitrer
	 * @param criteres les critères de filtre
	 */
	private void filterSexe(List<PersonnePhysique> list, CriteresPersonne criteres) {
		final Sexe sexeCritere = criteres.getSexe();
		if (sexeCritere != null) {
			CollectionUtils.filter(list, new Predicate() {
				@Override
				public boolean evaluate(Object object) {
					final PersonnePhysique pp = (PersonnePhysique) object;
					final Sexe sexe = tiersService.getSexe(pp);
					return (sexe == sexeCritere || sexe == null);
				}
			});
		}
	}

	/**
	 * Supprime toutes les personnes dont la date de naissances ne correspond pas avec celle spécifié dans le message
	 *
	 * @param list     la liste des personnes à fitrer
	 * @param criteres les critères de filtre
	 */
	private void filterDateNaissance(List<PersonnePhysique> list, CriteresPersonne criteres) {
		final RegDate critereDateNaissance = criteres.getDateNaissance();
		if (critereDateNaissance != null) {
			CollectionUtils.filter(list, new Predicate() {
				@Override
				public boolean evaluate(Object object) {
					return matchDateNaissance((PersonnePhysique) object, critereDateNaissance);
				}

			});
		}
	}

	/**
	 * verifie si la date de naissance du message et celui de la pp match
	 *
	 * @param pp                   la personne physique dont on veut vérifier la date de naissance.
	 * @param critereDateNaissance
	 * @return
	 */
	private boolean matchDateNaissance(PersonnePhysique pp, RegDate critereDateNaissance) {
		final RegDate dateNaissance = tiersService.getDateNaissance(pp);
		final RegDate dateLimite = RegDate.get(1901, 1, 1);
		if (dateNaissance != null && critereDateNaissance.isAfterOrEqual(dateLimite)) {
			//SIFISC-9006 les dates partiels doivent être prises en compte et comparées
			return dateNaissance.compareTo(critereDateNaissance)==0;
		}
		else {
			return true;
		}
	}

	@Override
	public Map<IdentificationContribuable.Etat, Integer> calculerStats(IdentificationContribuableCriteria identificationContribuableCriteria) {
		final Map<IdentificationContribuable.Etat, Integer> resultatStats = new EnumMap<>(IdentificationContribuable.Etat.class);
		for (IdentificationContribuable.Etat etat : IdentificationContribuable.Etat.values()) {
			identificationContribuableCriteria.setEtatMessage(etat.name());
			final int res = count(identificationContribuableCriteria, IdentificationContribuableEtatFilter.TOUS);
			resultatStats.put(etat, res);
		}
		return resultatStats;
	}

	@Override
	public String getNomCantonFromEmetteurId(String emetteurId) {

		final String sigle = getSigleCantonEmetteur(emetteurId);
		Canton canton = null;

		if (sigle != null) {
			try {
				canton = infraService.getCantonBySigle(sigle);
			}
			catch (ServiceInfrastructureException e) {
				// On n'a pas réussi à résoudre le canton,
				// on renvoie l'emetteur id telquel
				canton = null;
			}
		}

		if (canton != null && canton.getNomOfficiel() != null) {
			return canton.getNomOfficiel();
		}
		else {
			return emetteurId;
		}
	}

	@Override
	public IdentifiantUtilisateur getNomUtilisateurFromVisaUser(String visaUser) {

		String nom = visaUser;
		//user de l'identification automatique
		if (visaUser.contains("JMS-EvtIdentCtb")) {
			visaUser = "Traitement automatique";
			nom = visaUser;
		}
		else {
			final Operateur operateur = serviceSecuriteService.getOperateur(visaUser);
			if (operateur != null) {
				nom = operateur.getPrenom() + ' ' + operateur.getNom();
			}
		}

		return new IdentifiantUtilisateur(visaUser, nom);
	}

	@Override
	public boolean tenterIdentificationAutomatiqueContribuable(IdentificationContribuable message) throws Exception {
		// Ensuite : effectuer l'identification

		final Demande demande = message.getDemande();
		Assert.notNull(demande, "Le message ne contient aucune demande.");

		final CriteresPersonne criteresPersonne = demande.getPersonne();
		Assert.notNull(demande, "Le message ne contient aucun critère sur la personne à identifier.");
		if (criteresPersonne != null) {
			List<Long> found;
			try {
				found = identifie(criteresPersonne);
			}
			catch (TooManyIdentificationPossibilitiesException e) {
				found = Collections.emptyList();
			}
			if (found.size() == 1) {
				// on a trouvé un et un seul contribuable:
				final PersonnePhysique personne = (PersonnePhysique) tiersDAO.get(found.get(0));

				// on peut répondre immédiatement
				identifie(message, personne, Etat.TRAITE_AUTOMATIQUEMENT);
				return true;

			}
			else {
				//Dans le cas d'un message en exception,non traité automatiquement, on le met a traiter manuellement et on envoie une notification d'attente
				//si besoin
				if (Etat.EXCEPTION == message.getEtat()) {
					message.setEtat(Etat.A_TRAITER_MANUELLEMENT);
					//SIFISC-4873
					if (Demande.ModeIdentificationType.MANUEL_AVEC_ACK == demande.getModeIdentification()) {
						notifieAttenteIdentifManuel(message);
					}
				}
				return false;
			}
		}
		else {
			LOGGER.info(String.format("Le message %s ne contient aucun critère sur la personne à identifier, il est passé en traitement manuel.", message.getId()));
			//Dans le cas d'un message en exception,non traité automatiquement, on le met a traiter manuellement et on envoie une notification d'attente
			//si besoin
			if (Etat.EXCEPTION == message.getEtat()) {
				message.setEtat(Etat.A_TRAITER_MANUELLEMENT);
				//SIFISC-4873
				if (Demande.ModeIdentificationType.MANUEL_AVEC_ACK == demande.getModeIdentification()) {
					notifieAttenteIdentifManuel(message);
				}
			}

			return false;
		}
	}

	@Override
	public IdentifierContribuableResults relancerIdentificationAutomatique(RegDate dateTraitement, int nbThreads, StatusManager status, Long idMessage) {
		IdentifierContribuableProcessor processor = new IdentifierContribuableProcessor(this, hibernateTemplate, transactionManager, tiersService, adresseService);
		return processor.run(dateTraitement, nbThreads, status, idMessage);
	}

	@Override
	public synchronized void updateCriteres() {
		final IdentificationContribuableCache cache = new IdentificationContribuableCache();
		fillNewValuesForEmetteurIds(cache);
		fillNewValuesForPeriodesFiscales(cache);
		fillNewValuesForEtatsMessages(cache);
		fillNewValuesForTypesMessages(cache);
		cache.setListTraitementUsers(getNewValuesForTraitementUsers());

		// pas de besoin de synchronisation parce que l'assignement est atomique en java
		identificationContribuableCache = cache;
	}

	private static interface CustomValueFiller<T> {
		Map<Etat, List<T>> getValuesParEtat(IdentCtbDAO dao);
		void fillCache(IdentificationContribuableCache cache, Map<Etat, List<T>> values);
	}

	private <T> void fillValues(IdentificationContribuableCache cache, final CustomValueFiller<T> filler) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final Map<Etat, List<T>> map = template.execute(new TransactionCallback<Map<Etat, List<T>>>() {
			@Override
			public Map<Etat, List<T>> doInTransaction(TransactionStatus transactionStatus) {
				return filler.getValuesParEtat(identCtbDAO);
			}
		});

		// assignation des valeurs
		filler.fillCache(cache, map);
	}

	private void fillNewValuesForEmetteurIds(IdentificationContribuableCache cache) {
		fillValues(cache, new CustomValueFiller<String>() {
			@Override
			public Map<Etat, List<String>> getValuesParEtat(IdentCtbDAO dao) {
				return dao.getEmetteursIds();
			}

			@Override
			public void fillCache(IdentificationContribuableCache cache, Map<Etat, List<String>> values) {
				cache.setEmetteursIds(values);
			}
		});
	}

	private void fillNewValuesForTypesMessages(final IdentificationContribuableCache cache) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final Map<TypeDemande, Map<Etat, List<String>>> map = template.execute(new TransactionCallback<Map<TypeDemande, Map<Etat, List<String>>>>() {
			@Override
			public Map<TypeDemande, Map<Etat, List<String>>> doInTransaction(TransactionStatus status) {
				return identCtbDAO.getTypesMessages();
			}
		});

		cache.setTypesMessages(map);
	}

	private List<String> getNewValuesForTraitementUsers() {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		// on est appelé dans un thread Quartz -> pas de transaction ouverte par défaut
		return template.execute(new TransactionCallback<List<String>>() {
			@Override
			public List<String> doInTransaction(TransactionStatus status) {
					return identCtbDAO.getTraitementUser();

			}
		});
	}

	private void fillNewValuesForPeriodesFiscales(IdentificationContribuableCache cache) {
		fillValues(cache, new CustomValueFiller<Integer>() {
			@Override
			public Map<Etat, List<Integer>> getValuesParEtat(IdentCtbDAO dao) {
				return dao.getPeriodesFiscales();
			}

			@Override
			public void fillCache(IdentificationContribuableCache cache, Map<Etat, List<Integer>> map) {
				cache.setPeriodesFiscales(map);
			}
		});
	}

	@Override
	public Collection<String> getEmetteursId(IdentificationContribuableEtatFilter filter) {
		return identificationContribuableCache.getEmetteurIds(filter);
	}

	@Override
	public Collection<String> getTypesMessages(IdentificationContribuableEtatFilter filter) {
		return identificationContribuableCache.getTypesMessages(filter);
	}

	@Override
	public Collection<String> getTypeMessages(IdentificationContribuableEtatFilter filter, TypeDemande... typesDemande) {
		if (typesDemande != null && typesDemande.length > 0) {
			return identificationContribuableCache.getTypesMessagesParTypeDemande(filter, typesDemande);
		}
		else {
			return getTypesMessages(filter);
		}
	}

	@Override
	public Collection<Integer> getPeriodesFiscales(IdentificationContribuableEtatFilter filter) {
		return identificationContribuableCache.getPeriodesFiscales(filter);
	}

	@Override
	public List<String> getTraitementUser() {
		return identificationContribuableCache.getListTraitementUsers();
	}

	private void fillNewValuesForEtatsMessages(IdentificationContribuableCache cache) {
		fillValues(cache, new CustomValueFiller<Etat>() {
			@Override
			public Map<Etat, List<Etat>> getValuesParEtat(IdentCtbDAO dao) {
				return dao.getEtats();
			}

			@Override
			public void fillCache(IdentificationContribuableCache cache, Map<Etat, List<Etat>> map) {
				cache.setEtats(map);
			}
		});
	}

	@Override
	public Collection<Etat> getEtats(IdentificationContribuableEtatFilter filter) {
		return identificationContribuableCache.getEtats(filter);
	}

	private String traduireBusinessUser(String user) {
		String visaUser = user;
		if (visaUser.contains("JMS-EvtIdentCtb")) {
			visaUser = "Unireg";
		}

		return visaUser;
	}
}
