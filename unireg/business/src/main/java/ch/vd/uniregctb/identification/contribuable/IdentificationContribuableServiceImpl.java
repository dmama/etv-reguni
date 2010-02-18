package ch.vd.uniregctb.identification.contribuable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesFiscales;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresAdresse;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresPersonne;
import ch.vd.uniregctb.evenement.identification.contribuable.Demande;
import ch.vd.uniregctb.evenement.identification.contribuable.DemandeHandler;
import ch.vd.uniregctb.evenement.identification.contribuable.Erreur;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentCtbDAO;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableCriteria;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableMessageHandler;
import ch.vd.uniregctb.evenement.identification.contribuable.Reponse;
import ch.vd.uniregctb.evenement.identification.contribuable.Erreur.TypeErreur;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeRechercheLocalitePays;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeTiers;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeVisualisation;
import ch.vd.uniregctb.type.Sexe;

public class IdentificationContribuableServiceImpl implements IdentificationContribuableService, DemandeHandler {

	private static final Logger LOGGER = Logger.getLogger(IdentificationContribuableServiceImpl.class);

	private GlobalTiersSearcher searcher;
	private TiersDAO tiersDAO;
	private IdentCtbDAO identCtbDAO;
	private TiersService tiersService;
	private AdresseService adresseService;
	private ServiceInfrastructureService infraService;
	private IdentificationContribuableMessageHandler messageHandler;
	private static String REPARTITION_INTERCANTONALE="ssk-3001-000101";
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

	@SuppressWarnings( {
		"UnusedDeclaration"
	})
	private enum Phase {

		/**
		 * [UNIREG-1636] Première phase de recherche qui tient compte du numéro AVS 13 uniquement
		 */
		AVEC_NO_AVS_13,

		/**
		 * deuxième phase de recherche qui tient compte de tous les autres critères, le navs13 n'ayant rien donné
		 */
		COMPLET

	}

	/**
	 * {@inheritDoc}
	 */
	public List<PersonnePhysique> identifie(CriteresPersonne criteres) {

		// Recherche dans l'indexeur

		List<TiersIndexedData> indexedData = null;

		// [UNIREG-1636] effectue la recherche en plusieurs phases
		for (Phase phase : Phase.values()) {
			final TiersCriteria criteria = asTiersCriteria(criteres, phase);
			indexedData = searcher.search(criteria);
			if (indexedData != null && !indexedData.isEmpty()) {
				break;
			}
		}

		if (indexedData == null || indexedData.isEmpty()) {
			return Collections.emptyList();
		}

		// Restriction aux personnes physiques

		List<PersonnePhysique> list = new ArrayList<PersonnePhysique>();

		for (TiersIndexedData d : indexedData) {
			final Tiers t = tiersDAO.get(d.getNumero());
			if (t != null && t instanceof PersonnePhysique) {
				list.add((PersonnePhysique) t);
			}
		}

		// Restriction selon les autres critères

		list = filterSexe(list, criteres);
		list = filterAdresse(list, criteres);
		list = filterDateNaissance(list, criteres);

		return list;
	}



	/**
	 * Sauve la demande en base, identifie le ou les contribuables et retourne une réponse immédiatement si un seul contribuable est trouvé.
	 * Dans tous les autres cas (0, >1 ou en cas d'erreur), la demande est stockée pour traitement manuel.
	 */
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
	 * @param message
	 *            la requête d'identification initiale
	 * @param personne
	 *            la personne physique identifiée
	 * @param etat
	 *            le mode d'identification (manuel ou automatique)
	 * @throws Exception
	 *             si ça a pas marché
	 */
	private void identifie(IdentificationContribuable message, PersonnePhysique personne, Etat etat) throws Exception {
		Assert.notNull(personne);

		// [UNIREG-1911] On retourne le numéro du ménage-commun associé s'il existe
		Long mcId = null;
		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(personne, null);
		if (ensemble != null) {
			mcId = ensemble.getMenage().getNumero();
		}
		//[UNIREG-1940] On met à jour le contribuable si
		//-	le contribuable trouvé est « non habitant »
		//-	le message sur lequel a porté l’identification est une « répartition intercantonale »
		//-	le NPA de l’adresse contenue dans le message est un NPA du canton d’où provient le message
		verifierEtMettreAjourContribuable(message,personne);

		Reponse reponse = new Reponse();
		reponse.setDate(new Date());
		reponse.setNoContribuable(personne.getNumero());
		reponse.setNoMenageCommun(mcId);

		IdentificationContribuable messageReponse = new IdentificationContribuable();
		messageReponse.setId(message.getId());
		messageReponse.setHeader(message.getHeader());
		messageReponse.setNbContribuablesTrouves(1);
		messageReponse.setReponse(reponse);
		messageReponse.setEtat(etat);

		message.setNbContribuablesTrouves(1);
		message.setReponse(reponse);
		message.setEtat(etat);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Le message n°" + messageReponse.getId() + " est passé dans l'état [" + etat
					+ "]. Numéro du contribuable trouvé = " + personne.getNumero());
		}

		messageHandler.sendReponse(messageReponse);
	}

	/**
	 * Verifie et met a jour le contribuable avec les données contenus dans le message
	 * @param message
	 * @param personne
	 * @throws InfrastructureException
	 */

	private void verifierEtMettreAjourContribuable(IdentificationContribuable message, PersonnePhysique personne) throws InfrastructureException {
	if (!personne.isHabitant() &&
			REPARTITION_INTERCANTONALE.equals(message.getDemande().getTypeMessage()) &&
			messageFromCanton(message)) {
		CriteresPersonne criteres = message.getDemande().getPersonne();

		if (criteres.getNAVS13()!=null) {
			personne.setNumeroAssureSocial(criteres.getNAVS13());
		}

		if(criteres.getDateNaissance()!=null){
			personne.setDateNaissance(criteres.getDateNaissance());
		}
		if(criteres.getDateNaissance()!=null){
			personne.setDateNaissance(criteres.getDateNaissance());
		}
		if(criteres.getNom()!=null){
			personne.setNom(criteres.getNom());
		}
		if(criteres.getPrenoms()!=null){
			personne.setPrenom(criteres.getPrenoms());
		}
		if(criteres.getSexe()!=null){
			personne.setSexe(criteres.getSexe());
		}


	}


	}
/**
 * Permet de savoir si le NPA de l’adresse contenue dans le message est un NPA du canton d’où provient le message
 * @param message
 * @return
 * @throws InfrastructureException
 */
	private boolean messageFromCanton(IdentificationContribuable message) throws InfrastructureException {
		String emetteur = message.getDemande().getEmetteurId();
		Integer npa =null;
		if( message.getDemande().getPersonne().getAdresse()!=null){
			npa =  message.getDemande().getPersonne().getAdresse().getNpaSuisse();
		}
		String sigle = StringUtils.substring(emetteur,2,4);


		if (npa!=null) {
			Localite localite = infraService.getLocaliteByNPA(npa);
			if (sigle.equals(localite.getCommuneLocalite().getSigleCanton())) {
				return true;
			}
			else{
				return false;
			}

		}
		else{
			return false;
		}


	}

	/**
	 * Envoie une réponse <b>lorsqu'un contribuable n'a définitivement pas été identifié</b>.
	 *
	 * @param message
	 *            la requête d'identification initiale
	 * @param messageRetour
	 *            TODO
	 * @throws Exception
	 *             si ça a pas marché
	 */
	private void nonIdentifie(IdentificationContribuable message, Erreur erreur) throws Exception {

		final Etat etat = Etat.NON_IDENTIFIE; // par définition

		Reponse reponse = new Reponse();
		reponse.setDate(new Date());
		reponse.setErreur(erreur);
		IdentificationContribuable messageReponse = new IdentificationContribuable();
		messageReponse.setId(message.getId());
		messageReponse.setHeader(message.getHeader());
		messageReponse.setNbContribuablesTrouves(0);
		messageReponse.setReponse(reponse);
		messageReponse.setEtat(etat);

		message.setNbContribuablesTrouves(0);
		message.setReponse(reponse);
		message.setEtat(etat);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Le message n°" + messageReponse.getId() + " est passé dans l'état [" + etat + "]. Aucun contribuable trouvé.");
		}

		messageHandler.sendReponse(messageReponse);
	}

	/**
	 * Recherche une liste d'IdentificationContribuable en fonction de critères
	 *
	 * @param identificationContribuableCriteria
	 * @param paramPagination
	 * @return
	 */
	public List<IdentificationContribuable> find(IdentificationContribuableCriteria identificationContribuableCriteria,
			ParamPagination paramPagination, boolean nonTraiteOnly, boolean archiveOnly, boolean nonTraiterAndSuspendu) {
		return identCtbDAO.find(identificationContribuableCriteria, paramPagination, nonTraiteOnly, archiveOnly, nonTraiterAndSuspendu);
	}

	/**
	 * Nombre d'IdentificationContribuable en fonction de critères
	 *
	 * @param identificationContribuableCriteria
	 * @return
	 */
	public int count(IdentificationContribuableCriteria identificationContribuableCriteria, boolean nonTraiteOnly, boolean archiveOnly,
			boolean nonTraiterAndSuspendu) {
		return identCtbDAO.count(identificationContribuableCriteria, nonTraiteOnly, archiveOnly, nonTraiterAndSuspendu);
	}

	/**
	 * Force l'identification du contribuable
	 *
	 * @param identificationContribuable
	 * @param personne
	 * @throws Exception
	 */
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
	public void soumettre(IdentificationContribuable message) {
		// Ensuite : effectuer l'identification
		try {
			final Demande demande = message.getDemande();
			Assert.notNull(demande, "Le message ne contient aucune demande.");

			final CriteresPersonne criteresPersonne = demande.getPersonne();
			Assert.notNull(demande, "Le message ne contient aucune critère sur la personne à identifier.");

			final List<PersonnePhysique> list = identifie(criteresPersonne);
			if (list.size() == 1) {
				// on a trouvé un et un seul contribuable:
				PersonnePhysique personne = list.get(0);

				// on peut répondre immédiatement
				identifie(message, personne, Etat.TRAITE_AUTOMATIQUEMENT);

			}
			else {
				// dans tous les autres cas, on part en traitement manuel
				message.setNbContribuablesTrouves(list.size());
				message.setEtat(Etat.A_TRAITER_MANUELLEMENT);

				if (LOGGER.isDebugEnabled()) {
					final List<Long> ids = new ArrayList<Long>(list.size());
					for (PersonnePhysique pp : list) {
						ids.add(pp.getNumero());
					}
					LOGGER.debug("Le message n°" + message.getId() + " doit être traité manuellement. "
							+ "Nombre de contribuable(s) trouvé(s) = " + list.size() + " (" + ArrayUtils.toString(ids.toArray()) + ")");
				}
			}
		}
		catch (Exception e) {
			LOGGER.warn("Exception lors du traitement du message n°" + message.getId() + ". Le message sera traité manuellement.", e);

			// toute exception aura pour conséquence de provoquer un traitement manuel: on n'envoie donc pas de réponse immédiatement, et
			// on stocke le message d'erreur dans le champs reponse.erreur.message pas commodité.
			Reponse reponse = new Reponse();
			reponse.setErreur(new Erreur(TypeErreur.TECHNIQUE, null, e.getMessage()));

			message.setNbContribuablesTrouves(null);
			message.setReponse(reponse);
			message.setEtat(Etat.EXCEPTION);
		}
	}

	/**
	 * Impossible à identifier
	 *
	 * @param identificationContribuable
	 * @throws Exception
	 */
	public void impossibleAIdentifier(IdentificationContribuable identificationContribuable, Erreur erreur) throws Exception {
		nonIdentifie(identificationContribuable, erreur);
	}

	/**
	 * Converti le critère sur la personne en un critère compréhensible par l'indexeur.
	 *
	 * @param criteres
	 *            les critères sur la personne
	 * @param phase
	 *            la phase courante de recherche
	 * @return un critère de recherche compréhensible par le moteur d'indexation
	 */
	private TiersCriteria asTiersCriteria(CriteresPersonne criteres, Phase phase) {

		final TiersCriteria criteria = new TiersCriteria();

		final String navs13 = criteres.getNAVS13();

		if (Phase.AVEC_NO_AVS_13.equals(phase)) {
			if (navs13 != null) {
				criteria.setNumeroAVS(navs13);
			}

			else {
				updateCriteriaComplet(criteres, criteria);
			}

		}
		else {
			updateCriteriaComplet(criteres, criteria);
		}

		return criteria;
	}

	private void updateCriteriaComplet(CriteresPersonne criteres, final TiersCriteria criteria) {
		final String navs11 = criteres.getNAVS11();
		if (navs11 != null) {
			criteria.setNumeroAVS(navs11);
		}
		// [UNIREG-1630] dans tous les cas, on doit tenir compte des autres critères (autres que le numéro AVS, donc)
		criteria.setDateNaissance(criteres.getDateNaissance());
		final CriteresAdresse adresse = criteres.getAdresse();
		if (adresse != null) {
			criteria.setLocaliteOuPays(adresse.getLocalite());
		}
		criteria.setNomRaison(concatCriteres(criteres.getPrenoms(), criteres.getNom()));
		criteria.setTypeRechercheDuNom(TypeRecherche.EST_EXACTEMENT);
		criteria.setTypeRechercheDuPaysLocalite(TypeRechercheLocalitePays.LOCALITE);

		// critères statiques
		criteria.setInclureI107(false);
		criteria.setInclureTiersAnnules(false);
		criteria.setTypeTiers(TypeTiers.PERSONNE_PHYSIQUE);
		criteria.setTypeVisualisation(TypeVisualisation.COMPLETE);
	}

	/**
	 * Supprime toutes les personnes de sexe différent de celui spécifié
	 *
	 * @param list
	 *            la liste des personnes à fitrer
	 * @param criteres
	 *            les critères de filtre
	 * @return la liste d'entrée filtrée
	 */
	private List<PersonnePhysique> filterSexe(List<PersonnePhysique> list, CriteresPersonne criteres) {

		final Sexe sexeCritere = criteres.getSexe();
		if (sexeCritere != null) {
			CollectionUtils.filter(list, new Predicate() {
				public boolean evaluate(Object object) {
					final PersonnePhysique pp = (PersonnePhysique) object;
					final Sexe sexe = tiersService.getSexe(pp);
					return (sexe == sexeCritere || sexe==null);
				}
			});
		}

		return list;
	}

	/**
	 * Supprime toutes les personnes dont les adresses ne correspondent pas avec l'adresse spécifiée
	 *
	 * @param list
	 *            la liste des personnes à fitrer
	 * @param criteres
	 *            les critères de filtre
	 * @return la liste d'entrée filtrée
	 */
	private List<PersonnePhysique> filterAdresse(List<PersonnePhysique> list, CriteresPersonne criteres) {

		final CriteresAdresse adresseCritere = criteres.getAdresse();
		if (adresseCritere != null) {
			CollectionUtils.filter(list, new Predicate() {
				public boolean evaluate(Object object) {
					return matchAdresses((PersonnePhysique) object, adresseCritere);
				}
			});
		}
		return list;
	}
	/**Supprime toutes les personnes dont la date de naissances ne correspond pas avec celle spécifié dans le message
	 *
	* @param list
	 *            la liste des personnes à fitrer
	 * @param criteres
	 *            les critères de filtre
	 * @return la liste d'entrée filtrée
	 */
	private List<PersonnePhysique> filterDateNaissance(List<PersonnePhysique> list, CriteresPersonne criteres) {
		final RegDate critereDateNaissance = criteres.getDateNaissance();
		if (critereDateNaissance!=null) {
			CollectionUtils.filter(list, new Predicate() {
				public boolean evaluate(Object object) {
					return matchDateNaissance((PersonnePhysique) object, critereDateNaissance);
				}

			});
		}
		return list;
	}

	/** verifie si la date de naissance du message et celui de la pp match
	 *
	 * @param pp
	 * 			  la personne physique dont on veut vérifier la date de naissance.
	 * @param critereDateNaissance
	 * @return
	 */
	private boolean matchDateNaissance(PersonnePhysique pp, RegDate critereDateNaissance) {
		RegDate dateNaissance = tiersService.getDateNaissance(pp);
		RegDate dateLimite = RegDate.get(1901,1,1);
		if (dateNaissance !=null && critereDateNaissance.isAfterOrEqual(dateLimite)) {
			return dateNaissance.equals(critereDateNaissance);
		}
		else{
			return true;
		}

	}

	/**
	 * Vérifie les adresses fiscales d'une personne physique en fonction de critères, et détermine si elles correspondent.
	 *
	 * @param pp
	 *            la personne physique dont on veut vérifier les adresses fiscales.
	 * @param adresseCritere
	 *            les critères d'adresse
	 * @return <b>vrai</b> si une des adresses fiscales (courrier, représentation, domicile, poursuite) correspond aux critères d'adresse
	 *         spécifié.
	 */
	protected boolean matchAdresses(PersonnePhysique pp, CriteresAdresse adresseCritere) {

		final AdressesFiscales adresses;
		try {
			adresses = adresseService.getAdressesFiscales(pp, null, false);
		}
		catch (AdresseException e) {
			LOGGER.warn("Impossible de calculer les adresses du contribuable n°" + pp.getNumero() + ": on l'ignore.", e);
			return false;
		}

		return matchAdresseGenerique(adresses.courrier, adresseCritere) || matchAdresseGenerique(adresses.domicile, adresseCritere)
				|| matchAdresseGenerique(adresses.representation, adresseCritere)
				|| matchAdresseGenerique(adresses.poursuite, adresseCritere);
	}

	/**
	 * @param adresse
	 *            l'adresse générique à vérifier
	 * @param adresseCritere
	 *            les critères de vérification de l'adresse
	 * @return <b>vrai</b> si l'adresse générique corresponds aux critères d'adresse spécifié.
	 */
	private boolean matchAdresseGenerique(AdresseGenerique adresse, CriteresAdresse adresseCritere) {

		if (!StringUtils.isEmpty(adresseCritere.getChiffreComplementaire())) {
			LOGGER.warn("La comparaison sur le chiffre complémentaire n'est pas possible; le critère est ignoré.");
		}

		// test des différents critères en commençant par les plus déterminants
		return matchPays(adresse, adresseCritere) && // ------------------------------------------------------
				matchNumeroOrdrePostal(adresse, adresseCritere) && // ----------------------------------------
				matchNpa(adresse, adresseCritere) && // ------------------------------------------------------
				matchLocalite(adresse, adresseCritere) && // -------------------------------------------------
				matchRue(adresse, adresseCritere) && // ------------------------------------------------------
				matchCasePostale(adresse, adresseCritere) && // ----------------------------------------------
				matchNumeroPolice(adresse, adresseCritere) && // ---------------------------------------------
				matchComplement(adresse, adresseCritere) && // -----------------------------------------------
				matchNumeroAppartement(adresse, adresseCritere);
	}

	private boolean matchCasePostale(AdresseGenerique adresse, CriteresAdresse adresseCritere) {

		final String texteCasePostale = adresseCritere.getTexteCasePostale();
		final Integer numeroCasePostale = adresseCritere.getNumeroCasePostale();
		if (StringUtils.isEmpty(texteCasePostale) || numeroCasePostale == null) {
			return true; // pas de critère valable
		}

		final String casePostale = adresse.getCasePostale();
		if (StringUtils.isEmpty(casePostale)) {
			return false; // critère non respecté
		}

		final String critereCasePostale = texteCasePostale + " " + numeroCasePostale;
		return casePostale.equalsIgnoreCase(critereCasePostale);
	}

	@SuppressWarnings( {
		"SimplifiableIfStatement"
	})
	private boolean matchRue(AdresseGenerique adresse, CriteresAdresse adresseCritere) {

		final String critereRue = adresseCritere.getRue();
		if (StringUtils.isEmpty(critereRue)) {
			return true; // pas de critère valable
		}

		final String rue = adresse.getRue();
		if (StringUtils.isEmpty(rue)) {
			return false; // critère non respecté
		}

		return rue.equalsIgnoreCase(critereRue);
	}

	private boolean matchNpa(AdresseGenerique adresse, CriteresAdresse adresseCritere) {

		final String npaEtranger = adresseCritere.getNpaEtranger();
		final Integer npaSuisse = adresseCritere.getNpaSuisse();
		if (StringUtils.isEmpty(npaEtranger) && npaSuisse == null) {
			return true; // pas de critère valable
		}

		final String npa = adresse.getNumeroPostal();
		if (StringUtils.isEmpty(npa)) {
			return false; // critère non respecté
		}

		final String critereNpa = (npaSuisse == null ? npaEtranger : String.valueOf(npaSuisse));
		return npa.equalsIgnoreCase(critereNpa);
	}

	@SuppressWarnings( {
		"SimplifiableIfStatement"
	})
	private boolean matchNumeroPolice(AdresseGenerique adresse, CriteresAdresse adresseCritere) {

		final String critereNumero = adresseCritere.getNoPolice();
		if (StringUtils.isEmpty(critereNumero)) {
			return true; // pas de critère valable
		}

		final String numero = adresse.getNumero();
		if (StringUtils.isEmpty(numero)) {
			return false; // critère non respecté
		}

		return numero.equalsIgnoreCase(critereNumero);
	}

	private boolean matchNumeroOrdrePostal(AdresseGenerique adresse, CriteresAdresse adresseCritere) {

		final Integer critereNumero = adresseCritere.getNoOrdrePosteSuisse();
		if (critereNumero == null) {
			return true; // pas de critère valable
		}

		final int numero = adresse.getNumeroOrdrePostal();
		return numero == critereNumero;
	}

	@SuppressWarnings( {
		"SimplifiableIfStatement"
	})
	private boolean matchNumeroAppartement(AdresseGenerique adresse, CriteresAdresse adresseCritere) {

		final String critereNumero = adresseCritere.getNoAppartement();
		if (StringUtils.isEmpty(critereNumero)) {
			return true; // pas de critère valable
		}

		final String numero = adresse.getNumeroAppartement();
		if (StringUtils.isEmpty(numero)) {
			return false; // critère non respecté
		}

		return numero.equalsIgnoreCase(critereNumero);
	}

	private boolean matchLocalite(AdresseGenerique adresse, CriteresAdresse adresseCritere) {

		final String critereLocalite = adresseCritere.getLieu();
		if (StringUtils.isEmpty(critereLocalite)) {
			return true; // pas de critère valable
		}

		final String localite = adresse.getLocalite();
		final String localiteComplete = adresse.getLocaliteComplete();
		if (StringUtils.isEmpty(localite) && StringUtils.isEmpty(localiteComplete)) {
			return false; // critère non respecté
		}

		// on est souple sur les localité : si on match une des deux (localité abbregée ou localité complète), on considère que cela suffit
		return localite.equalsIgnoreCase(critereLocalite) || localiteComplete.equalsIgnoreCase(critereLocalite);
	}

	private boolean matchComplement(AdresseGenerique adresse, CriteresAdresse adresseCritere) {

		final String critereComplement1 = adresseCritere.getLigneAdresse1();
		final String critereComplement2 = adresseCritere.getLigneAdresse2();
		if (StringUtils.isEmpty(critereComplement1) && StringUtils.isEmpty(critereComplement2)) {
			return true; // pas de critère valable
		}

		final String complement = adresse.getComplement();
		if (StringUtils.isEmpty(complement)) {
			return false; // critère non respecté
		}

		// on est souple sur les compléments : si on match un des deux, on considère que cela suffit (la probabilité est déjà suffisemment
		// faible, puisqu'il s'agit de free-text).
		return complement.equalsIgnoreCase(critereComplement1) || complement.equalsIgnoreCase(critereComplement2);
	}

	private boolean matchPays(AdresseGenerique adresse, CriteresAdresse adresseCritere) {

		final String criterePays = adresseCritere.getCodePays();
		if (StringUtils.isEmpty(criterePays)) {
			return true; // pas de critère valable
		}

		final Integer noOfsPays = adresse.getNoOfsPays();
		if (noOfsPays == null) {
			return false; // critère non respecté
		}

		final Pays pays;
		try {
			pays = infraService.getPays(noOfsPays);
		}
		catch (InfrastructureException e) {
			LOGGER.warn("Impossible de trouver le pays avec le numéro OFS " + noOfsPays + ": on l'ignore.", e);
			return false;
		}
		if (pays == null) {
			return false; // critère non respecté
		}

		final String codePays = pays.getSigleOFS();
		if (codePays == null) {
			LOGGER.warn("Le code du pays avec le numéro OFS " + noOfsPays + " est nul: on l'ignore.");
			return false;
		}

		return codePays.equalsIgnoreCase(criterePays);
	}

	private static String concatCriteres(final String first, final String second) {
		final String concat;
		if (first != null || second != null) {
			StringBuilder s = new StringBuilder();
			if (first != null) {
				s.append(first);
			}
			if (first != null && second != null) {
				s.append(' ');
			}
			if (second != null) {
				s.append(second);
			}
			concat = s.toString();
		}
		else {
			concat = null;
		}
		return concat;
	}

	public Map<IdentificationContribuable.Etat, Integer> calculerStats(IdentificationContribuableCriteria identificationContribuableCriteria) {
		int res = 0;
		Map<IdentificationContribuable.Etat, Integer> resultatStats = new HashMap<IdentificationContribuable.Etat, Integer>();

		for (IdentificationContribuable.Etat etat : IdentificationContribuable.Etat.values()) {

			identificationContribuableCriteria.setEtatMessage(etat.name());
			res = count(identificationContribuableCriteria, false, false, false);
			resultatStats.put(etat, res);

		}
		return resultatStats;

	}
}
