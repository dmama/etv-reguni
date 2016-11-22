package ch.vd.uniregctb.identification.contribuable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.upi.ServiceUpiException;
import ch.vd.unireg.interfaces.upi.ServiceUpiRaw;
import ch.vd.unireg.interfaces.upi.data.UpiPersonInfo;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.DefaultThreadFactory;
import ch.vd.uniregctb.common.DefaultThreadNameGenerator;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.NumeroCtbStringRenderer;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.common.StringComparator;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresAdresse;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresEntreprise;
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
import ch.vd.uniregctb.interfaces.service.host.Operateur;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.Sexe;

public class IdentificationContribuableServiceImpl implements IdentificationContribuableService, DemandeHandler, InitializingBean, DisposableBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdentificationContribuableServiceImpl.class);

	private static final String REPARTITION_INTERCANTONALE = "ssk-3001-000101";
	private static final StringRenderer<Long> NO_CTB_RENDERER = new NumeroCtbStringRenderer();

	private GlobalTiersSearcher searcher;
	private TiersDAO tiersDAO;
	private IdentCtbDAO identCtbDAO;
	private TiersService tiersService;
	private AdresseService adresseService;
	private ServiceInfrastructureService infraService;
	private IdentificationContribuableMessageHandler messageHandler;
	private PlatformTransactionManager transactionManager;
	private ServiceSecuriteService serviceSecuriteService;
	private ServiceUpiRaw serviceUpi;
	private HibernateTemplate hibernateTemplate;
	private int flowSearchThreadPoolSize;

	private IdentificationContribuableCache identificationContribuableCache = new IdentificationContribuableCache();        // cache vide à l'initialisation
	private ExecutorService asynchronousFlowSearchExecutor;

	private Set<String> caracteresSpeciauxIdentificationEntreprise = Collections.emptySet();
	private Set<Pattern> motsReservesIdentificationEntreprise = Collections.emptySet();

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

	public void setServiceUpi(ServiceUpiRaw serviceUpi) {
		this.serviceUpi = serviceUpi;
	}

	public void setFlowSearchThreadPoolSize(int flowSearchThreadPoolSize) {
		this.flowSearchThreadPoolSize = flowSearchThreadPoolSize;
	}

	public void setCaracteresSpeciauxIdentificationEntreprise(Set<String> caracteresSpeciauxIdentificationEntreprise) {
		if (caracteresSpeciauxIdentificationEntreprise == null || caracteresSpeciauxIdentificationEntreprise.isEmpty()) {
			this.caracteresSpeciauxIdentificationEntreprise = Collections.emptySet();
		}
		else {
			this.caracteresSpeciauxIdentificationEntreprise = caracteresSpeciauxIdentificationEntreprise;
		}
	}

	public void setMotsReservesIdentificationEntreprise(Set<String> motsReservesIdentificationEntreprise) {
		if (motsReservesIdentificationEntreprise == null || motsReservesIdentificationEntreprise.isEmpty()) {
			this.motsReservesIdentificationEntreprise = Collections.emptySet();
		}
		else {
			this.motsReservesIdentificationEntreprise = new LinkedHashSet<>(motsReservesIdentificationEntreprise.size());
			for (String mr : motsReservesIdentificationEntreprise) {
				final Pattern pattern = Pattern.compile(String.format("\\b%s\\b", Pattern.quote(mr)), Pattern.CASE_INSENSITIVE);
				this.motsReservesIdentificationEntreprise.add(pattern);
			}
		}
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
					LOGGER.info("Préchargement des valeurs pour les critères de recherche pour l'identification des contribuables.");
					try {
						updateCriteres();
					}
					catch (Throwable t) {
						LOGGER.error("Exception lors du pré-chargement des valeurs pour les critères de recherche des messages d'identification de contribuable", t);
					}
					finally {
						LOGGER.info("Préchargement terminé.");
					}
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

	private enum PhaseRechercheSurNomPrenom {

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

	private enum PhaseRechercheSurRaisonSociale {

		/**
		 * Première phase de recherche sur la raison sociale
		 */
		STANDARD,

		/**
		 * Phase en enlevant les caractères spéciaux
		 */
		SANS_CARACTERES_SPECIAUX,

		/**
		 * Phase qui consiste à franciser les ä, ö, et ü en ae, oe et ue
		 */
		FRANCISATION,

		/**
		 * Phase qui consiste à enlever quelques mots réservés des critères de la demande
		 */
		SANS_MOTS_RESERVES
	}

	private static <T> List<Long> buildIdList(List<T> src, Function<T, Long> idFetcher) {
		if (src == null) {
			return Collections.emptyList();
		}

		return src.stream()
				.map(idFetcher)
				.collect(Collectors.toList());
	}

	private static List<Long> buildIdListFromPP(List<PersonnePhysique> list) {
		return buildIdList(list, PersonnePhysique::getNumero);
	}

	private static List<Long> buildIdListFromIndex(List<TiersIndexedData> list) {
		return buildIdList(list, TiersIndexedData::getNumero);
	}

	public static boolean criteresEmptyForReChercheComplete(CriteresPersonne criteres) {
		return criteres.getAdresse() == null &&
				criteres.getDateNaissance() == null &&
				criteres.getSexe() == null &&
				StringUtils.isBlank(criteres.getNom()) &&
				StringUtils.isBlank(criteres.getPrenoms()) &&
				StringUtils.isBlank(criteres.getNAVS11()) ;
	}

	public static boolean criteresEmptyForReChercheComplete(CriteresEntreprise criteres) {
		return criteres.getAdresse() == null && StringUtils.isBlank(criteres.getRaisonSociale());
	}

	@Override
	public List<Long> identifiePersonnePhysique(CriteresPersonne criteres, Mutable<String> upiAutreNavs) throws TooManyIdentificationPossibilitiesException {

		final Mutable<String> avsUpi = upiAutreNavs != null ? upiAutreNavs : new MutableObject<>();
		avsUpi.setValue(null);

		// 1. phase AVS13
		final List<TiersIndexedData> indexedAvs13 = findByNavs13(criteres, avsUpi);
		if (indexedAvs13 != null && !indexedAvs13.isEmpty()) {
			final List<PersonnePhysique> ppList = getListePersonneFromIndexedData(indexedAvs13);
			final List<PersonnePhysique> listeFiltree = filterCoherenceAfterIdentificationAvs13(ppList, criteres);
			if (isIdentificationOK(listeFiltree)) {
				LOGGER.info("Identification par phase avs13 réussie.");
				return buildIdListFromPP(listeFiltree);
			}
		}

		//Si aucun autre critères que le navs13 n'est renseigné, on s'arrete la.
		if (criteresEmptyForReChercheComplete(criteres)) {
			LOGGER.info("Phase de recherche complète non effectuée car les critères nécessaires à la recherche sont vides.");
			return Collections.emptyList();
		}

		// 2. si rien trouvé d'unique, on passe à la phase noms/prénoms...
		LOGGER.info("Début de phase de recherche complète");
		final List<TiersIndexedData> indexedComplets = findAvecCriteresComplets(criteres, avsUpi.getValue(), NB_MAX_RESULTS_POUR_LISTE_IDENTIFICATION);
		return buildIdListFromIndex(indexedComplets);
	}

	@Override
	public List<Long> identifieEntreprise(CriteresEntreprise criteres) throws TooManyIdentificationPossibilitiesException {
		// 1. phase IDE
		final List<TiersIndexedData> indexedIde = findByIde(criteres);
		if (indexedIde != null && indexedIde.size() == 1) {
			// 1 seul résultat -> c'est peut-être lui
			LOGGER.info("L'identification par numéro IDE a fourni un résultat : " + FormatNumeroHelper.numeroCTBToDisplay(indexedIde.get(0).getNumero()));
			if (checkRaisonSocialeEntreprise(criteres.getRaisonSociale(), indexedIde.get(0))) {
				// c'est lui !
				LOGGER.info("Identification par phase IDE réussie.");
				return buildIdListFromIndex(indexedIde);
			}
			else {
				LOGGER.info("Identification par numéro IDE rejetée pour cause de raisons sociales différentes");
			}
		}

		// 2. phase numéro RC
		final List<TiersIndexedData> indexedNumeroRC = findByNumeroRC(criteres);
		if (indexedNumeroRC != null && indexedNumeroRC.size() == 1) {
			// 1 seul résultat, c'est peut-être lui,,,
			LOGGER.info("L'identification par numéro RC a fourni un résultat : " + FormatNumeroHelper.numeroCTBToDisplay(indexedNumeroRC.get(0).getNumero()));
			if (checkRaisonSocialeEntreprise(criteres.getRaisonSociale(), indexedNumeroRC.get(0))) {
				// ok, c'est lui !
				LOGGER.info("Identification par phase NuméroRC réussie.");
				return buildIdListFromIndex(indexedNumeroRC);
			}
			else {
				LOGGER.info("Identification par numéro RC rejetée pour cause de raisons sociales différentes");
			}
		}

		// si aucun autre critère que le numéro IDE n'est renseigné, pas la peine d'aller plus loin
		if (criteresEmptyForReChercheComplete(criteres)) {
			LOGGER.info("Phase de recherche complète non effectuée car les critères nécessaires à la recherche sont vides.");
			return Collections.emptyList();
		}

		// 2. phase raison sociale / npa
		LOGGER.info("Début de phase de recherche complète");
		final List<TiersIndexedData> indexedComplets = findAvecCriteresComplets(criteres, NB_MAX_RESULTS_POUR_LISTE_IDENTIFICATION);
		return buildIdListFromIndex(indexedComplets);
	}

	private List<TiersIndexedData> findByIde(CriteresEntreprise criteres) {
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setTypesTiersImperatifs(EnumSet.of(TiersCriteria.TypeTiers.AUTRE_COMMUNAUTE, TiersCriteria.TypeTiers.ENTREPRISE));
		criteria.setNumeroIDE(criteres.getIde());
		if (!criteria.isEmpty()) {
			try {
				return searcher.search(criteria);
			}
			catch (TooManyResultsIndexerException e) {
				// dans la phase IDE, trop de résultats = aucun résultat
			}
		}
		return Collections.emptyList();
	}

	private List<TiersIndexedData> findByNumeroRC(CriteresEntreprise criteres) {
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setTypesTiersImperatifs(EnumSet.of(TiersCriteria.TypeTiers.ENTREPRISE));
		criteria.setNumeroRC(criteres.getNumeroRC());
		if (!criteria.isEmpty()) {
			try {
				return searcher.search(criteria);
			}
			catch (TooManyResultsIndexerException e) {
				// dans la phase 'numéro RC', trop de résultats = aucun résultat
			}
		}
		return Collections.emptyList();
	}

	private boolean checkRaisonSocialeEntreprise(String askedRaisonSociale, @NotNull TiersIndexedData found) {
		if (StringUtils.isBlank(askedRaisonSociale)) {
			// pas de besoin spécifique, on accepte...
			return true;
		}

		final Tiers tiers = tiersDAO.get(found.getNumero());
		final String knownRaisonSociale = getDerniereRaisonSocialeEntreprise(tiers);
		return knownRaisonSociale != null && containedIn(knownRaisonSociale, askedRaisonSociale);
	}

	private boolean containedIn(String container, String containee) {
		final String normalizedContainer = normalize(container);
		final String normalizedContainee = normalize(containee);
		return normalizedContainer.contains(normalizedContainee);
	}

	/**
	 * @param str chaîne de caractères source
	 * @return chaîne de caractères dans laquelle tout est en minuscules, sans espaces multiples ni caractères accentués
	 */
	@NotNull
	private String normalize(String str) {
		if (str == null) {
			return StringUtils.EMPTY;
		}
		final String strippedDown = enleverMotsReserves(franciser(enleverCaracteresSpeciaux(str, null), null), null);
		final String lowercases = StringComparator.toLowerCaseWithoutAccent(strippedDown);
		return lowercases.replaceAll("\\s+", " ").trim();
	}

	private String getDerniereRaisonSocialeEntreprise(Tiers tiers) {
		if (tiers instanceof Entreprise) {
			return tiersService.getDerniereRaisonSociale((Entreprise) tiers);
		}
		else if (tiers instanceof AutreCommunaute) {
			return ((AutreCommunaute) tiers).getNom();
		}
		else {
			return null;
		}
	}

	private List<PersonnePhysique> filterCoherenceAfterIdentificationAvs13(List<PersonnePhysique> list, CriteresPersonne criteres) {
		//SIFISC-10914
		//Le controle ne se fera que sur le nom /prenom si on a pas de date de naissance dans la demande
		if (criteres.getDateNaissance()==null) {
			return controleNomPrenom(list, criteres);
		}

		//SIFISC-13033
		//Contrôle des résultats avec la date de naissance qui est renseigné.on passe une liste de copie
		// si le controle est ko on a notre liste de départ qui est intacte

		final List<PersonnePhysique> listPourControleDateNaissance = new ArrayList<>(list);
		final List<PersonnePhysique> listPourControleNomPrenom = new ArrayList<>(list);
		filterDateNaissance(listPourControleDateNaissance, criteres);
		if (isIdentificationOK(listPourControleDateNaissance)) {
			return listPourControleDateNaissance;
		}
		else {
			//Controle des résultats avec le nom et le prénom
			return controleNomPrenom(listPourControleNomPrenom, criteres);
		}
	}

	/**
	 * Effectue le contrôle de cohérence sur le nom prénom
	 * @param list liste de personne sphysiques à vérifier
	 * @param criteres de recherche tel qu'indiqué dans la demande notamment avec le nom / prenom
	 * @return liste des personnes correspondantes aux nom /prenom ou liste vide si aucune personne ne correspond
	 */
	private List<PersonnePhysique> controleNomPrenom(List<PersonnePhysique> list, CriteresPersonne criteres) {
		filterNomPrenom(list, criteres);
		if (isIdentificationOK(list)) {
			return list;
		}
		else {
			return Collections.emptyList();
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

	/**
	 * Fait une recherche selon le numéro avs13 donné dans les critères. Si ce numéro ne donne aucun résultat, on
	 * appelle alors le service UPI pour voir s'il n'a pas mieux à proposer (auquel cas on refait la recherche
	 * avec ce numéro et on le renvoie à l'appelant)
	 * @param criteres critères issus de la demande d'identification
	 * @param navs13UpiProposal en sortie, si non-null et si l'appel UPI a donné un nouveau numéro, le numéro en question
	 * @return la liste des tiers trouvés
	 */
	private List<TiersIndexedData> findByNavs13(CriteresPersonne criteres, @NotNull Mutable<String> navs13UpiProposal) {
		try {
			final String navs13Demande = criteres.getNAVS13();
			final List<TiersIndexedData> first = findByNavs13(navs13Demande);
			if (first.isEmpty() && StringUtils.isNotBlank(navs13Demande)) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(String.format("Aucun résultat trouvé avec le NAVS13 '%s'. On essaie à l'UPI.", navs13Demande));
				}

				// recherche à l'UPI
				final UpiPersonInfo upiInfo = serviceUpi.getPersonInfo(navs13Demande);
				if (upiInfo != null && !navs13Demande.equals(upiInfo.getNoAvs13())) {
					LOGGER.info(String.format("L'UPI indique que le NAVS13 '%s' a été remplacé par '%s'.", navs13Demande, upiInfo.getNoAvs13()));

					// information pour l'appelant
					navs13UpiProposal.setValue(upiInfo.getNoAvs13());

					// recherche avec le nouveau numéro
					return findByNavs13(upiInfo.getNoAvs13());
				}
				else if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(String.format("Réponse de l'UPI pour le NAVS13 '%s' : pas mieux !", navs13Demande));
				}
			}
			return first;
		}
		catch (TooManyResultsIndexerException e) {
			return Collections.emptyList();
		}
		catch (ServiceUpiException e) {
			LOGGER.warn("Erreur à l'appel au service UPI, l'identification se poursuit sans l'aide des informations UPI.", e);
			return Collections.emptyList();
		}
	}

	private List<TiersIndexedData> findByNavs13(String navs13) throws TooManyResultsIndexerException {
		final TiersCriteria criteria = asTiersCriteriaNAVS13(navs13);
		if (!criteria.isEmpty()) {
			try {
				return searcher.search(criteria);
			}
			catch (TooManyResultsIndexerException e) {
				throw e;
			}
			catch (IndexerException e) {
				throw new RuntimeException(e);
			}
		}
		return Collections.emptyList();
	}

	/**
	 * Phase de recherche sur tous les critères (parce que la phase sur les critères IDE a échoué)
	 * @param criteres les critères de l'identification tels que fournis dans la demande
	 * @param maxNumberForList le nombre maximal de donner à renvoyer au delà duquel tout se termine en {@link TooManyIdentificationPossibilitiesException}
	 * @return une liste de contribuables qui satisfont aux critères
	 * @throws TooManyIdentificationPossibilitiesException si le nombre de résultats trouvés est plus grand que <code>maxNumberForList</code>
	 */
	@NotNull
	private List<TiersIndexedData> findAvecCriteresComplets(CriteresEntreprise criteres, int maxNumberForList) throws TooManyIdentificationPossibilitiesException {

		List<TiersIndexedData> indexedData = null;

		for (PhaseRechercheSurRaisonSociale phase : PhaseRechercheSurRaisonSociale.values()) {
			try {
				final TiersCriteria criteria = asTiersCriteriaForRaisonSociale(criteres.getRaisonSociale(), phase);
				if (criteres.getAdresse() != null) {
					final CriteresAdresse adresse = criteres.getAdresse();
					final String npa = adresse.getNpaSuisse() == null ? StringUtils.trimToNull(adresse.getNpaEtranger()) : Integer.toString(adresse.getNpaSuisse());
					if (StringUtils.isNotBlank(npa)) {
						criteria.setNpaTousOrNull(npa);
					}
				}

				if (!criteria.isEmpty()) {
					indexedData = searcher.searchTop(criteria, maxNumberForList + 1);
					if (indexedData != null && !indexedData.isEmpty()) {
						if (indexedData.size() > maxNumberForList) {
							throw new TooManyIdentificationPossibilitiesException(maxNumberForList, indexedData);
						}
						break;
					}
				}
			}
			catch (IgnoredPhaseException e) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(String.format("Phase %s ignorée : %s", phase, e.getMessage()));
				}
			}
		}

		if (indexedData == null || indexedData.isEmpty() || isIdeNonConforme(indexedData, criteres.getIde())) {
			return Collections.emptyList();
		}
		return indexedData;
	}

	private TiersCriteria asTiersCriteriaForRaisonSociale(String critereRaisonSociale, PhaseRechercheSurRaisonSociale phase) throws IgnoredPhaseException {
		final String consigne;
		final Mutable<Boolean> actionEffective = new MutableBoolean(true);
		switch (phase) {
		case STANDARD:
			consigne = critereRaisonSociale;
			break;
		case SANS_CARACTERES_SPECIAUX:
			consigne = enleverCaracteresSpeciaux(critereRaisonSociale, actionEffective);
			break;
		case FRANCISATION:
			consigne = franciser(enleverCaracteresSpeciaux(critereRaisonSociale, null), actionEffective);
			break;
		case SANS_MOTS_RESERVES:
			consigne = enleverMotsReserves(franciser(enleverCaracteresSpeciaux(critereRaisonSociale, null), null), actionEffective);
			break;
		default:
			throw new IllegalArgumentException("Invalid phase : " + phase);
		}

		if (actionEffective.getValue() == null || !actionEffective.getValue()) {
			throw new IgnoredPhaseException("La raison sociale n'est pas affectée par cette transformation.");
		}

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNomRaison(consigne);
		criteria.setTypesTiersImperatifs(EnumSet.of(TiersCriteria.TypeTiers.AUTRE_COMMUNAUTE, TiersCriteria.TypeTiers.ENTREPRISE));
		return criteria;
	}

	/**
	 * En fait, on les remplace par des séparations de mots
	 * @param source chaîne de caractères en entrée
	 * @param actionEffective si non <code>null</code>, contiendra en sortie un flag qui indique si oui ou non des caractères ont été enlevés
	 * @return chaîne de caractères dans laquelle les caractères spéciaux ont été enlevés
	 */
	private String enleverCaracteresSpeciaux(String source, @Nullable Mutable<Boolean> actionEffective) {
		if (StringUtils.isBlank(source) || caracteresSpeciauxIdentificationEntreprise.isEmpty()) {
			if (actionEffective != null) {
				actionEffective.setValue(false);
			}
			return source;
		}

		String currentState = source;
		for (String cs : caracteresSpeciauxIdentificationEntreprise) {
			if (currentState.contains(cs)) {
				currentState = currentState.replace(cs, " ");
			}
		}
		if (actionEffective != null) {
			//noinspection StringEquality
			actionEffective.setValue(currentState != source);
		}
		return currentState;
	}

	private static final Pattern UMLAEUTE_PATTEN = Pattern.compile("[äüöÄÜÖ]", Pattern.CASE_INSENSITIVE);

	private static String franciser(String source, @Nullable Mutable<Boolean> actionEffective) {
		final String res;
		if (StringUtils.isBlank(source)) {
			res = source;
		}
		else {
			final Matcher matcher = UMLAEUTE_PATTEN.matcher(source);
			if (matcher.find()) {
				res = source.replaceAll("[äÄ]", "ae").replaceAll("[üÜ]", "ue").replaceAll("[öÖ]", "oe");
			}
			else {
				res = source;
			}
		}

		if (actionEffective != null) {
			//noinspection StringEquality
			actionEffective.setValue(res != source);
		}
		return res;
	}

	private String enleverMotsReserves(String source, @Nullable Mutable<Boolean> actionEffective) {
		if (StringUtils.isBlank(source) || motsReservesIdentificationEntreprise.isEmpty()) {
			if (actionEffective != null) {
				actionEffective.setValue(false);
			}
			return source;
		}

		String currentState = source;
		for (Pattern mr : motsReservesIdentificationEntreprise) {
			final Matcher matcher = mr.matcher(currentState);
			if (matcher.find()) {
				currentState = matcher.replaceAll(StringUtils.EMPTY);
			}
		}
		if (actionEffective != null) {
			//noinspection StringEquality
			actionEffective.setValue(currentState != source);
		}
		return currentState;
	}

	/**
	 * Phase de recherche sur tous les critères (parce que la phase sur les critères NAVS a échoué)
	 * @param criteres les critères de l'identification tels que fournis dans la demande
	 * @param avsUpi si non-null, le numéro AVS renvoyé par l'UPI en remplacement de celui présent dans la demande
	 * @param maxNumberForList le nombre maximal de donner à renvoyer au delà duquel tout se termine en {@link TooManyIdentificationPossibilitiesException}
	 * @return une liste de contribuables qui satisfont aux critères
	 * @throws TooManyIdentificationPossibilitiesException si le nombre de résultats trouvés est plus grand que <code>maxNumberForList</code>
	 */
	@NotNull
	private List<TiersIndexedData> findAvecCriteresComplets(CriteresPersonne criteres, @Nullable String avsUpi, int maxNumberForList) throws TooManyIdentificationPossibilitiesException {

		List<TiersIndexedData> indexedData = null;
		String controleNavs13 = null;

		// [SIFISC-147] effectue la recherche sur les nom et prénoms en plusieurs phase
		for (PhaseRechercheSurNomPrenom phase : PhaseRechercheSurNomPrenom.values()) {
			try {
				final TiersCriteria criteria = asTiersCriteriaForNomPrenom(criteres, phase);
				if (criteres.getNAVS11() != null) {
					// on ne compare que les 8 premiers caractères
					criteria.setNavs11OrNull(StringUtils.left(criteres.getNAVS11(), 8));
				}
				if (criteres.getSexe() != null) {
					criteria.setSexe(criteres.getSexe());
				}
				if (criteres.getDateNaissance() != null) {
					criteria.setDateNaissanceInscriptionRC(criteres.getDateNaissance());
				}
				if (criteres.getAdresse() != null) {
					final CriteresAdresse adresse = criteres.getAdresse();
					final String npa = adresse.getNpaSuisse() == null ? StringUtils.trimToNull(adresse.getNpaEtranger()) : Integer.toString(adresse.getNpaSuisse());
					if (StringUtils.isNotBlank(npa)) {
						criteria.setNpaTousOrNull(npa);
					}
				}


				if (!criteria.isEmpty()) {
					indexedData = searcher.searchTop(criteria, maxNumberForList + 1);
					if (indexedData != null && !indexedData.isEmpty()) {
						if (indexedData.size() > maxNumberForList) {
							throw new TooManyIdentificationPossibilitiesException(maxNumberForList, indexedData);
						}
						break;
					}
				}
			}
			catch (IgnoredPhaseException e) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(String.format("Phase %s ignorée : %s", phase, e.getMessage()));
				}
			}
		}

		if (avsUpi != null) {
			controleNavs13 = avsUpi;
		}
		else if (criteres.getNAVS13() != null) {
			controleNavs13 = criteres.getNAVS13();
		}

		if (indexedData == null || indexedData.isEmpty() || isNavs13NonConforme(indexedData, controleNavs13)) {
			return Collections.emptyList();
		}
		return indexedData;
	}

	/**
	 * Contrôle final sur le NAVS13
	 */
	private static boolean isNavs13NonConforme(List<TiersIndexedData> tiersIndexedData, String navs13) {
		if (tiersIndexedData == null || tiersIndexedData.isEmpty() || StringUtils.isBlank(navs13)) {
			return false;
		}

		final String navs13Candidat = tiersIndexedData.get(0).getNavs13_1();
		final boolean resultatUnique = tiersIndexedData.size() == 1;
		final boolean navs13CandidatRenseigne = StringUtils.isNotEmpty(navs13Candidat);
		final boolean navs13Differents = !navs13.equals(navs13Candidat);
		return resultatUnique && navs13CandidatRenseigne && navs13Differents;
	}

	/**
	 * Contrôle final sur le numéro IDE
	 * @return <code>false</code> s'il n'y a pas exactement un résultat dans la liste, ou si ce résultat n'a pas d'IDE connu, ou si le critère donné sur l'IDE est vide&nbsp;; <code>true</code> sinon.
	 */
	private static boolean isIdeNonConforme(List<TiersIndexedData> tiersIndexData, String ide) {
		if (tiersIndexData == null || tiersIndexData.isEmpty() || tiersIndexData.size() > 1 || StringUtils.isBlank(ide)) {
			return false;
		}

		final TiersIndexedData found = tiersIndexData.get(0);
		final List<String> knownIdes = found.getNumerosIDE();
		if (knownIdes == null || knownIdes.isEmpty()) {
			return false;
		}

		for (String known : knownIdes) {
			if (ide.equalsIgnoreCase(known)) {
				return false;
			}
		}
		return true;
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
	 * Envoie une réponse d'identification positive de type automatique
	 * @param message  la requête d'identification initiale
	 * @param personne la personne physique identifiée
	 * @throws Exception en cas de problème
	 */
	private void identifieAutomatiquement(IdentificationContribuable message, PersonnePhysique personne) throws Exception {
		identifie(message, personne, Etat.TRAITE_AUTOMATIQUEMENT);
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
		messageReponse.setCommentaireTraitement(null);
		messageReponse.setReponse(reponse);
		messageReponse.setEtat(etat);
		messageReponse.getHeader().setBusinessUser(traduireBusinessUser(user));

		message.setNbContribuablesTrouves(1);
		message.setCommentaireTraitement(null);
		message.setReponse(reponse);
		message.setEtat(etat);
		message.setDateTraitement(DateHelper.getCurrentDate());
		message.setTraitementUser(user);

		LOGGER.info(String.format("Le message n°%d est passé dans l'état [%s]. Numéro du contribuable trouvé = %d", messageReponse.getId(), etat, personne.getNumero()));
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
				final RegDate dateDemande = RegDateHelper.get(message.getDemande().getDate());
				final List<Localite> localites = infraService.getLocalitesByNPA(npa, dateDemande);
				if (localites != null && !localites.isEmpty()) {
					final Set<String> siglesCantons = new HashSet<>(localites.size());
					for (Localite localite : localites) {
						final Commune commune = localite.getCommuneLocalite();
						if (commune != null) {
							siglesCantons.add(commune.getSigleCanton());
						}
					}
					return siglesCantons.contains(sigle);
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
		messageReponse.setCommentaireTraitement(null);
		messageReponse.setReponse(reponse);
		messageReponse.setEtat(etat);
		messageReponse.getHeader().setBusinessUser(traduireBusinessUser(user));

		message.setNbContribuablesTrouves(0);
		message.setCommentaireTraitement(null);
		message.setReponse(reponse);
		message.setEtat(etat);
		message.setDateTraitement(DateHelper.getCurrentDate());
		message.setTraitementUser(user);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(String.format("Le message n°%d est passé dans l'état [%s]. Aucun contribuable trouvé.", messageReponse.getId(), etat));
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

		final Reponse reponse = new Reponse();
		reponse.setDate(DateHelper.getCurrentDate());
		reponse.setEnAttenteIdentifManuel(true);
		message.setReponse(reponse);

		final IdentificationContribuable messageReponse = new IdentificationContribuable();
		messageReponse.setId(message.getId());
		messageReponse.setHeader(message.getHeader());
		messageReponse.setReponse(reponse);
		messageReponse.setEtat(etat);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(String.format("Le message n°%d est passé dans l'état [%s], le demandeur va en être notifié.", messageReponse.getId(), etat));
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

	private final Map<TypeDemande, Consumer<IdentificationContribuable>> MAP_SOUMISSION = buildMapSoumission();

	@NotNull
	private Map<TypeDemande, Consumer<IdentificationContribuable>> buildMapSoumission() {
		final Map<TypeDemande, Consumer<IdentificationContribuable>> map = new EnumMap<>(TypeDemande.class);
		map.put(TypeDemande.MELDEWESEN, this::soumettreMessageMeldewesen);
		map.put(TypeDemande.NCS, this::soumettreMessageNCS);
		map.put(TypeDemande.E_FACTURE, this::soumettreMessageEfacture);
		map.put(TypeDemande.IMPOT_SOURCE, this::soumettreMessageImpotSource);
		map.put(TypeDemande.RAPPROCHEMENT_RF, this::soumettreMessageRapprochementProprietaireRF);
		return Collections.unmodifiableMap(map);
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
		final Consumer<IdentificationContribuable> soumission = MAP_SOUMISSION.get(typeDemande);
		if (soumission != null) {
			soumission.accept(message);
		}
		else {
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

	private void soumettreMessageRapprochementProprietaireRF(IdentificationContribuable message) {
		LOGGER.info("Le message n°" + message.getId() + " passe en traitement Rapprochement RF");
		soumettreMessage(message);
	}

	private enum IdentificationResultKind {
		FOUND_NONE,
		FOUND_ONE,
		FOUND_SEVERAL,
		FOUND_MANY
	}

	private static String getMessageNonIdentification(List<Long> found, IdentificationResultKind resultKind) {
		final String str;
		if (resultKind == IdentificationResultKind.FOUND_ONE) {
			str = null;
		}
		else if (resultKind == IdentificationResultKind.FOUND_NONE) {
			str = "Aucun contribuable trouvé.";
		}
		else if (resultKind == IdentificationResultKind.FOUND_SEVERAL || resultKind == IdentificationResultKind.FOUND_MANY) {
			final StringBuilder b = new StringBuilder();
			if (resultKind == IdentificationResultKind.FOUND_SEVERAL) {
				b.append(found.size());
			}
			else {
				b.append("Plus de ").append(NB_MAX_RESULTS_POUR_LISTE_IDENTIFICATION);
			}
			b.append(" contribuables trouvés : ").append(CollectionsUtils.toString(found, NO_CTB_RENDERER, ", "));
			if (resultKind == IdentificationResultKind.FOUND_MANY) {
				b.append(", ...");
			}
			else {
				b.append(".");
			}
			str = b.toString();
		}
		else {
			throw new IllegalArgumentException("Valeur non prévue : " + resultKind);
		}
		return str;
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

			final Mutable<String> avsUpi = new MutableObject<>();
			IdentificationResultKind resultKind;
			List<Long> found;
			try {
				found = identifiePersonnePhysique(criteresPersonne, avsUpi);
				switch (found.size()) {
				case 0:
					resultKind = IdentificationResultKind.FOUND_NONE;
					break;
				case 1:
					resultKind = IdentificationResultKind.FOUND_ONE;
					break;
				default:
					resultKind = IdentificationResultKind.FOUND_SEVERAL;
					break;
				}
			}
			catch (TooManyIdentificationPossibilitiesException e) {
				found = e.getExamplesFound();
				resultKind = IdentificationResultKind.FOUND_MANY;
			}
			message.setNAVS13Upi(avsUpi.getValue());

			if (resultKind == IdentificationResultKind.FOUND_ONE) {
				// on a trouvé un et un seul contribuable:
				final PersonnePhysique personne = (PersonnePhysique) tiersDAO.get(found.get(0));

				// on peut répondre immédiatement
				identifieAutomatiquement(message, personne);
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
					message.setNbContribuablesTrouves(resultKind != IdentificationResultKind.FOUND_MANY ? found.size() : null);
					message.setCommentaireTraitement(getMessageNonIdentification(found, resultKind));
					message.setEtat(Etat.A_TRAITER_MANUELLEMENT);

					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug(String.format("Le message %d doit être traité manuellement. %s", message.getId(), message.getCommentaireTraitement()));
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
			// on stocke le message d'erreur dans le champs reponse.erreur.message par commodité dans le cas ou aucun accusé de reception n'est demandé
			final Reponse reponse = new Reponse();
			reponse.setErreur(new Erreur(TypeErreur.TECHNIQUE, null, e.getMessage()));
			message.setReponse(reponse);
		}

		message.setNbContribuablesTrouves(null);
		message.setCommentaireTraitement(null);
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
		return asTiersCriteriaNAVS13(criteres.getNAVS13());
	}

	private TiersCriteria asTiersCriteriaNAVS13(String navs13) {
		final TiersCriteria criteria = new TiersCriteria();
		IdentificationContribuableHelper.setUpCriteria(criteria);
		criteria.setNumeroAVS(navs13);
		return criteria;
	}

	private TiersCriteria asTiersCriteriaForNomPrenom(CriteresPersonne criteres, PhaseRechercheSurNomPrenom phase) throws IgnoredPhaseException {

		final TiersCriteria criteria = new TiersCriteria();
		switch (phase) {
		case STANDARD:
			IdentificationContribuableHelper.updateCriteriaStandard(criteres, criteria);
			break;
		case SANS_DERNIER_NOM:
			IdentificationContribuableHelper.updateCriteriaSansDernierNom(criteres, criteria);
			break;
		case SANS_DERNIER_PRENOM:
			IdentificationContribuableHelper.updateCriteriaSansDernierPrenom(criteres, criteria);
			break;
		case STANDARD_SANS_E:
			IdentificationContribuableHelper.updateCriteriaStandardSansE(criteres, criteria);
			break;
		case SANS_DERNIER_NOM_SANS_E:
			IdentificationContribuableHelper.updateCriteriaSansDernierNomSansE(criteres, criteria);
			break;
		case SANS_DERNIER_PRENOM_SANS_E:
			IdentificationContribuableHelper.updateCriteriaSansDernierPrenomSansE(criteres, criteria);
			break;

		}
		return criteria;
	}


	private List<PersonnePhysique> filterNomPrenom(List<PersonnePhysique> list, CriteresPersonne criteres) {
		final String nomCritere = criteres.getNom();
		final String prenomCritere = criteres.getPrenoms();
		if (nomCritere != null) {
			final String nomMinuscule = StringComparator.toLowerCaseWithoutAccent(nomCritere);
			CollectionUtils.filter(list, pp -> {
				final String nomPrenom = StringComparator.toLowerCaseWithoutAccent(tiersService.getNomPrenom(pp));
				return (nomPrenom.contains(nomMinuscule));
			});
		}

		if (prenomCritere != null) {
			final String prenomMinuscule = StringComparator.toLowerCaseWithoutAccent(prenomCritere);
			CollectionUtils.filter(list, pp -> {
				final String nomPrenom = StringComparator.toLowerCaseWithoutAccent(tiersService.getNomPrenom(pp));
				return (nomPrenom.contains(prenomMinuscule));
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
			CollectionUtils.filter(list, pp -> {
				final Sexe sexe = tiersService.getSexe(pp);
				return (sexe!=null && sexe == sexeCritere);
			});
		}
	}

	/**
	 * Supprime toutes les personnes dont la date de naissances ne correspond pas à celle spécifié dans le message
	 *
	 * @param list     la liste des personnes à fitrer
	 * @param criteres les critères de filtre
	 */
	private void filterDateNaissance(List<PersonnePhysique> list, CriteresPersonne criteres) {
		final RegDate critereDateNaissance = criteres.getDateNaissance();
		if (critereDateNaissance != null) {
			CollectionUtils.filter(list, pp -> matchDateNaissance(pp, critereDateNaissance));
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
		if (critereDateNaissance.isBefore(dateLimite) || critereDateNaissance.isAfter(RegDate.get())) {
			return true;
		}
		if (dateNaissance != null) {
			//SIFISC-9006 les dates partiels doivent être prises en compte et comparées
			return dateNaissance.compareTo(critereDateNaissance)==0;
		}
		else {
			return false;
		}
	}

	@Override
	public Map<IdentificationContribuable.Etat, Integer> calculerStats(IdentificationContribuableCriteria identificationContribuableCriteria) {
		final Map<IdentificationContribuable.Etat, Integer> resultatStats = new EnumMap<>(IdentificationContribuable.Etat.class);
		for (IdentificationContribuable.Etat etat : IdentificationContribuable.Etat.values()) {
			identificationContribuableCriteria.setEtatMessage(etat);
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
		if (criteresPersonne != null) {
			final Mutable<String> avsUpi = new MutableObject<>();
			List<Long> found;
			IdentificationResultKind resultKind;
			try {
				found = identifiePersonnePhysique(criteresPersonne, avsUpi);
				switch (found.size()) {
				case 0:
					resultKind = IdentificationResultKind.FOUND_NONE;
					break;
				case 1:
					resultKind = IdentificationResultKind.FOUND_ONE;
					break;
				default:
					resultKind = IdentificationResultKind.FOUND_SEVERAL;
					break;
				}
			}
			catch (TooManyIdentificationPossibilitiesException e) {
				found = e.getExamplesFound();
				resultKind = IdentificationResultKind.FOUND_MANY;
			}
			message.setNAVS13Upi(avsUpi.getValue());

			// un résultat trouvé -> on a réussi !
			if (resultKind == IdentificationResultKind.FOUND_ONE) {
				// on a trouvé un et un seul contribuable:
				final PersonnePhysique personne = (PersonnePhysique) tiersDAO.get(found.get(0));

				// on peut répondre immédiatement
				identifieAutomatiquement(message, personne);
				return true;
			}

			// pas ou trop de résultats -> mettre à jour la valeur associée à l'erreur
			message.setNbContribuablesTrouves(resultKind != IdentificationResultKind.FOUND_MANY ? found.size() : null);
			message.setCommentaireTraitement(getMessageNonIdentification(found, resultKind));
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(String.format("Le message %d doit être traité manuellement. %s", message.getId(), message.getCommentaireTraitement()));
			}
		}
		else {
			LOGGER.info(String.format("Le message %d ne contient aucun critère sur la personne à identifier, il est passé en traitement manuel.", message.getId()));
			message.setNbContribuablesTrouves(null);
			message.setCommentaireTraitement("Aucun critère.");
		}

		//Dans le cas d'un message en exception,non traité automatiquement, on le met a traiter manuellement et on envoie une notification d'attente si besoin
		if (Etat.EXCEPTION == message.getEtat()) {

			message.setEtat(Etat.A_TRAITER_MANUELLEMENT);
			//SIFISC-4873
			if (Demande.ModeIdentificationType.MANUEL_AVEC_ACK == demande.getModeIdentification()) {
				notifieAttenteIdentifManuel(message);
			}
		}
		return false;
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

	private interface CustomValueFiller<T> {
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
