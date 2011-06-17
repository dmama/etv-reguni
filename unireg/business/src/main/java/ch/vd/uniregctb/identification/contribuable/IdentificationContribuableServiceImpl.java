package ch.vd.uniregctb.identification.contribuable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.securite.model.Operateur;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdressesFiscales;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.CasePostale;
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
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableMessageHandler;
import ch.vd.uniregctb.evenement.identification.contribuable.Reponse;
import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.TooManyResultsIndexerException;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.model.Canton;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeRecherche;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeTiers;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeVisualisation;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;

public class IdentificationContribuableServiceImpl implements IdentificationContribuableService, DemandeHandler {

	private static final Logger LOGGER = Logger.getLogger(IdentificationContribuableServiceImpl.class);

	private GlobalTiersSearcher searcher;
	private TiersDAO tiersDAO;
	private IdentCtbDAO identCtbDAO;
	private TiersService tiersService;
	private AdresseService adresseService;
	private ServiceInfrastructureService infraService;
	private IdentificationContribuableMessageHandler messageHandler;
	private PlatformTransactionManager transactionManager;
	private static final String REPARTITION_INTERCANTONALE = "ssk-3001-000101";
	private ServiceSecuriteService serviceSecuriteService;

	public PlatformTransactionManager getTransactionManager() {
		return transactionManager;
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

	@SuppressWarnings({
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
	@Override
	public List<PersonnePhysique> identifie(CriteresPersonne criteres) {

		// Recherche dans l'indexeur

		List<TiersIndexedData> indexedData = null;
		Phase phaseSucces = null;

		// [UNIREG-1636] effectue la recherche en plusieurs phases
		for (Phase phase : Phase.values()) {
			final TiersCriteria criteria = asTiersCriteria(criteres, phase);
			if (!criteria.isEmpty()) {

				try {
					indexedData = searcher.search(criteria);
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

			if (indexedData != null && !indexedData.isEmpty()) {
				phaseSucces = phase;
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
		list = filterNavs11(list,criteres);
		list = filterSexe(list, criteres);
		list = filterDateNaissance(list, criteres);
		if (Phase.COMPLET == phaseSucces) {
			list = filterAdresse(list, criteres);
		}


		return list;
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
		Long mcId = null;
		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(personne, null);
		if (ensemble != null) {
			mcId = ensemble.getMenage().getNumero();
		}
		// [UNIREG-1940] On met à jour le contribuable si
		// - le contribuable trouvé est « non habitant »
		// - le message sur lequel a porté l’identification est une « répartition intercantonale »
		// - le NPA de l’adresse contenue dans le message est un NPA du canton d’où provient le message
		verifierEtMettreAJourContribuable(message, personne);

		Reponse reponse = new Reponse();
		reponse.setDate(DateHelper.getCurrentDate());
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
		message.setDateTraitement(DateHelper.getCurrentDate());
		String user = AuthenticationHelper.getCurrentPrincipal();
		message.setTraitementUser(user);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Le message n°" + messageReponse.getId() + " est passé dans l'état [" + etat
					+ "]. Numéro du contribuable trouvé = " + personne.getNumero());
		}

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
				&& messageFromCanton(message)) {
			CriteresPersonne criteres = message.getDemande().getPersonne();

			if (criteres.getNAVS13() != null) {
				personne.setNumeroAssureSocial(criteres.getNAVS13());
			}
		}

	}

	/**
	 * Permet de mettre a jour si besoin les adresses du non non habitant avec le contenu de l'adresse inclu dans le message
	 *
	 * @param message
	 * @param personne
	 * @throws ServiceInfrastructureException
	 */
	private void mettreAJourAdresseNonHabitant(IdentificationContribuable message, PersonnePhysique personne)
			throws ServiceInfrastructureException {
		CriteresAdresse criteresAdresse = message.getDemande().getPersonne().getAdresse();
		if (criteresAdresse != null) {
			// Calcul de l'ONRP
			Integer onrp = criteresAdresse.getNoOrdrePosteSuisse();
			if (onrp == null) {
				Localite localite = infraService.getLocaliteByNPA(criteresAdresse.getNpaSuisse());
				if (localite != null) {
					onrp = localite.getNoOrdre();
				}
			}
			//On a trouvé l'onrp, Oh Joie !!!! on peut tenter de mettre à jour l'adresse
			if (onrp != null) {
				// on verifie les adresses du non Habitant
				AdresseSuisse adresseCourrier = (AdresseSuisse) personne.getAdresseActive(TypeAdresseTiers.COURRIER, null);
				if (adresseCourrier == null) {
					adresseCourrier = new AdresseSuisse();
					setUpAdresse(message, criteresAdresse, onrp, adresseCourrier);
					personne.addAdresseTiers(adresseCourrier);
				}
				else {
					//l'adresse est juste mis a jour
					setUpAdresse(message, criteresAdresse, onrp, adresseCourrier);
				}

			}

		}

	}

	private void setUpAdresse(IdentificationContribuable message, CriteresAdresse criteresAdresse, Integer onrp,
	                          AdresseSuisse adresseCourrier) {
		adresseCourrier.setUsage(TypeAdresseTiers.COURRIER);
		adresseCourrier.setDateDebut(RegDate.get(message.getLogCreationDate()));
		String complement = null;
		if (criteresAdresse.getLigneAdresse1() != null) {
			complement = criteresAdresse.getLigneAdresse1();

		}
		if (criteresAdresse.getLigneAdresse2() != null) {
			complement = complement + " " + criteresAdresse.getLigneAdresse2();

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
	private boolean messageFromCanton(IdentificationContribuable message) throws ServiceInfrastructureException {
		String emetteur = message.getDemande().getEmetteurId();
		Integer npa = null;
		if (message.getDemande().getPersonne().getAdresse() != null) {
			npa = message.getDemande().getPersonne().getAdresse().getNpaSuisse();
		}
		String sigle = StringUtils.substring(emetteur, 2, 4);

		if (npa != null) {
			Localite localite = infraService.getLocaliteByNPA(npa);
			if (sigle.equals(localite.getCommuneLocalite().getSigleCanton())) {
				return true;
			}
			else {
				return false;
			}

		}
		else {
			return false;
		}

	}

	/**
	 * Envoie une réponse <b>lorsqu'un contribuable n'a définitivement pas été identifié</b>.
	 *
	 * @param message       la requête d'identification initiale
	 * @param messageRetour TODO
	 * @throws Exception si ça a pas marché
	 */
	private void nonIdentifie(IdentificationContribuable message, Erreur erreur) throws Exception {

		final Etat etat = Etat.NON_IDENTIFIE; // par définition

		Reponse reponse = new Reponse();
		reponse.setDate(DateHelper.getCurrentDate());
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
		message.setDateTraitement(DateHelper.getCurrentDate());
		String user = AuthenticationHelper.getCurrentPrincipal();
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
	 * @param identificationContribuableCriteria
	 *
	 * @param paramPagination
	 * @param typeDemande
	 * @return
	 */
	@Override
	public List<IdentificationContribuable> find(IdentificationContribuableCriteria identificationContribuableCriteria,
	                                             ParamPagination paramPagination, boolean nonTraiteOnly, boolean archiveOnly, boolean nonTraiterAndSuspendu, TypeDemande typeDemande) {
		return identCtbDAO.find(identificationContribuableCriteria, paramPagination, nonTraiteOnly, archiveOnly, nonTraiterAndSuspendu, typeDemande);
	}

	/**
	 * Nombre d'IdentificationContribuable en fonction de critères
	 *
	 * @param identificationContribuableCriteria
	 *
	 * @param typeDemande
	 * @return
	 */
	@Override
	public int count(IdentificationContribuableCriteria identificationContribuableCriteria, boolean nonTraiteOnly, boolean archiveOnly,
	                 boolean nonTraiterAndSuspendu, TypeDemande typeDemande) {
		return identCtbDAO.count(identificationContribuableCriteria, nonTraiteOnly, archiveOnly, nonTraiterAndSuspendu, typeDemande);
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
		default:
			traiterException(message, new IllegalArgumentException("Type de demande inconnue"));
		}


	}

	private void soumettreMessageNCS(IdentificationContribuable message) {
		// to be implemented...
	}

	private void soumettreMessageMeldewesen(IdentificationContribuable message) {
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
				//UNIREG 2412 Ajout de possibilités au service d'identification UniReg asynchrone

				if (Demande.ModeIdentificationType.SANS_MANUEL == demande.getModeIdentification()) {
					String contenuMessage = "Aucun contribuable n’a été trouvé avec l’identification automatique et l’identification manuelle n’a pas été demandée";
					Erreur erreur = new Erreur(TypeErreur.METIER, "01", contenuMessage);
					nonIdentifie(message, erreur);

				}
				else {
					if (Demande.ModeIdentificationType.MANUEL_AVEC_ACK == demande.getModeIdentification()) {
						notifieAttenteIdentifManuel(message);
					}

					// dans le cas MANUEL_AVEC_ACK et MANUEL_SANS_ACK le message est mis en traitement manuel
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
		}
		catch (Exception e) {
			traiterException(message, e);
		}
	}

	private void traiterException(IdentificationContribuable message, Exception e) {
		LOGGER.warn("Exception lors du traitement du message n°" + message.getId() + ". Le message sera traité manuellement.", e);

		// toute exception aura pour conséquence de provoquer un traitement manuel: on n'envoie donc pas de réponse immédiatement, et
		// on stocke le message d'erreur dans le champs reponse.erreur.message pas commodité.
		Reponse reponse = new Reponse();
		reponse.setErreur(new Erreur(TypeErreur.TECHNIQUE, null, e.getMessage()));

		message.setNbContribuablesTrouves(null);
		message.setReponse(reponse);
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

	/**
	 * Converti le critère sur la personne en un critère compréhensible par l'indexeur.
	 *
	 * @param criteres les critères sur la personne
	 * @param phase    la phase courante de recherche
	 * @return un critère de recherche compréhensible par le moteur d'indexation
	 */
	private TiersCriteria asTiersCriteria(CriteresPersonne criteres, Phase phase) {

		final TiersCriteria criteria = new TiersCriteria();

		final String navs13 = criteres.getNAVS13();

		if (Phase.AVEC_NO_AVS_13 == phase) {
			if (navs13 != null) {
				criteria.setNumeroAVS(navs13);
			}
		}
		else {
			updateCriteriaComplet(criteres, criteria);
		}

		return criteria;
	}

	private void updateCriteriaComplet(CriteresPersonne criteres, final TiersCriteria criteria) {

		// [UNIREG-1630] dans tous les cas, on doit tenir compte des autres critères (autres que le numéro AVS, donc)
		criteria.setNomRaison(concatCriteres(criteres.getPrenoms(), criteres.getNom()));
		criteria.setTypeRechercheDuNom(TypeRecherche.EST_EXACTEMENT);


		// critères statiques
		criteria.setInclureI107(false);
		criteria.setInclureTiersAnnules(false);
		criteria.setTypeTiers(TypeTiers.PERSONNE_PHYSIQUE);
		criteria.setTypeVisualisation(TypeVisualisation.COMPLETE);
	}

	/**
	 * Supprime toutes les personnes de sexe différent de celui spécifié
	 *
	 * @param list     la liste des personnes à fitrer
	 * @param criteres les critères de filtre
	 * @return la liste d'entrée filtrée
	 */
	private List<PersonnePhysique> filterSexe(List<PersonnePhysique> list, CriteresPersonne criteres) {

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

		return list;
	}

	/**
	 * Supprime toutes les personnes dont les adresses ne correspondent pas avec l'adresse spécifiée
	 *
	 * @param list     la liste des personnes à fitrer
	 * @param criteres les critères de filtre
	 * @return la liste d'entrée filtrée
	 */
	private List<PersonnePhysique> filterAdresse(List<PersonnePhysique> list, CriteresPersonne criteres) {

		final CriteresAdresse adresseCritere = criteres.getAdresse();
		if (adresseCritere != null) {
			CollectionUtils.filter(list, new Predicate() {
				@Override
				public boolean evaluate(Object object) {
					return matchAdresses((PersonnePhysique) object, adresseCritere);
				}
			});
		}
		return list;
	}

	/**
	 * Supprime toutes les personnes dont le navs11 ne correspond pas avec celle spécifié dans le message
	 *
	 * @param list     la liste des personnes à fitrer
	 * @param criteres les critères de filtre
	 * @return la liste d'entrée filtrée
	 */
	private List<PersonnePhysique> filterNavs11(List<PersonnePhysique> list, CriteresPersonne criteres) {
		final String criteresNAVS11 = criteres.getNAVS11();
		if (criteresNAVS11 != null){
			CollectionUtils.filter(list, new Predicate() {
				@Override
				public boolean evaluate(Object object) {
					return matchNavs11((PersonnePhysique) object, criteresNAVS11);
				}

			});
		}
		return list;
	}

	private boolean matchNavs11(PersonnePhysique object, String criteresNAVS11) {
		String navs11 = tiersService.getAncienNumeroAssureSocial(object);
		if(navs11!=null){
			// SIFISC-790
			// On ne considère que les 8 premiers chiffres du navs11 sans les points
			String debutNavs11 = navs11.substring(0,8);
			String debutCritere = criteresNAVS11.substring(0,8);
			if(debutCritere.equals(debutNavs11)){
				return true;
			}
			else{
				return false;
			}
		}
		return false;  //To change body of created methods use File | Settings | File Templates.
	}



	/**
	 * Supprime toutes les personnes dont la date de naissances ne correspond pas avec celle spécifié dans le message
	 *
	 * @param list     la liste des personnes à fitrer
	 * @param criteres les critères de filtre
	 * @return la liste d'entrée filtrée
	 */
	private List<PersonnePhysique> filterDateNaissance(List<PersonnePhysique> list, CriteresPersonne criteres) {
		final RegDate critereDateNaissance = criteres.getDateNaissance();
		if (critereDateNaissance != null) {
			CollectionUtils.filter(list, new Predicate() {
				@Override
				public boolean evaluate(Object object) {
					return matchDateNaissance((PersonnePhysique) object, critereDateNaissance);
				}

			});
		}
		return list;
	}

	/**
	 * verifie si la date de naissance du message et celui de la pp match
	 *
	 * @param pp                   la personne physique dont on veut vérifier la date de naissance.
	 * @param critereDateNaissance
	 * @return
	 */
	private boolean matchDateNaissance(PersonnePhysique pp, RegDate critereDateNaissance) {
		RegDate dateNaissance = tiersService.getDateNaissance(pp);
		RegDate dateLimite = RegDate.get(1901, 1, 1);
		if (dateNaissance != null && critereDateNaissance.isAfterOrEqual(dateLimite)) {
			return dateNaissance.equals(critereDateNaissance);
		}
		else {
			return true;
		}

	}

	/**
	 * Vérifie les adresses fiscales d'une personne physique en fonction de critères, et détermine si elles correspondent.
	 *
	 * @param pp             la personne physique dont on veut vérifier les adresses fiscales.
	 * @param adresseCritere les critères d'adresse
	 * @return <b>vrai</b> si une des adresses fiscales (courrier, représentation, domicile, poursuite) correspond aux critères d'adresse spécifié.
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
	 * @param adresse        l'adresse générique à vérifier
	 * @param adresseCritere les critères de vérification de l'adresse
	 * @return <b>vrai</b> si l'adresse générique corresponds aux critères d'adresse spécifié.
	 */
	private boolean matchAdresseGenerique(AdresseGenerique adresse, CriteresAdresse adresseCritere) {

		//On ne matche plus que sur le NPA
		// test des différents critères en commençant par les plus déterminants
		if (adresse != null) {
			return matchNpa(adresse, adresseCritere);
		}
		return true;
	}

	private boolean matchCasePostale(AdresseGenerique adresse, CriteresAdresse adresseCritere) {

		final String texteCasePostale = adresseCritere.getTexteCasePostale();
		final Integer numeroCasePostale = adresseCritere.getNumeroCasePostale();
		if (StringUtils.isEmpty(texteCasePostale) || numeroCasePostale == null) {
			return true; // pas de critère valable
		}

		final CasePostale casePostale = adresse.getCasePostale();
		if (casePostale == null) {
			return false; // critère non respecté
		}

		final String critereCasePostale = texteCasePostale + " " + numeroCasePostale;
		return casePostale.toString().equalsIgnoreCase(critereCasePostale);
	}

	@SuppressWarnings({
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
			return true; // critère non respecté
		}

		final String critereNpa = (npaSuisse == null ? npaEtranger : String.valueOf(npaSuisse));
		return npa.equalsIgnoreCase(critereNpa);
	}

	@SuppressWarnings({
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

	@SuppressWarnings({
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
		catch (ServiceInfrastructureException e) {
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

	@Override
	public Map<IdentificationContribuable.Etat, Integer> calculerStats(IdentificationContribuableCriteria identificationContribuableCriteria, TypeDemande typeDemande) {
		int res = 0;
		Map<IdentificationContribuable.Etat, Integer> resultatStats = new HashMap<IdentificationContribuable.Etat, Integer>();

		for (IdentificationContribuable.Etat etat : IdentificationContribuable.Etat.values()) {

			identificationContribuableCriteria.setEtatMessage(etat.name());
			res = count(identificationContribuableCriteria, false, false, false, typeDemande);
			resultatStats.put(etat, res);

		}
		return resultatStats;

	}

	@Override
	public String getNomCantonFromEmetteurId(String emetteurId) {

		String sigle = StringUtils.substring(emetteurId, 2, 4);
		Canton canton = null;

		try {
			canton = infraService.getCantonBySigle(sigle);
		}
		catch (ServiceInfrastructureException e) {
			// On a pas réussi a resoudre le canton,
			//on renvoie l'emetteur id telquel
			canton = null;
		}

		if (canton != null && canton.getNomMinuscule() != null) {
			return canton.getNomMinuscule();
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
			Operateur operateur = serviceSecuriteService.getOperateur(visaUser);

			if (operateur != null) {
				nom = operateur.getPrenom() + " " + operateur.getNom();
			}
		}

		return new IdentifiantUtilisateur(visaUser,nom);
	}

	@Override
	public boolean tenterIdentificationAutomatiqueContribuable(IdentificationContribuable message) throws Exception {
		// Ensuite : effectuer l'identification

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
			return true;

		}
		else {
			//Dans le cas d'un message en exception,non traité automatiquement, on le met a traiter manuellement
			if (Etat.EXCEPTION == message.getEtat()) {
				message.setEtat(Etat.A_TRAITER_MANUELLEMENT);
			}
			return false;
		}

	}

	@Override
	public IdentifierContribuableResults relancerIdentificationAutomatique(RegDate dateTraitement, int nbThreads, StatusManager status, Long idMessage) {
		IdentifierContribuableProcessor processor = new IdentifierContribuableProcessor(this, identCtbDAO, transactionManager);
		return processor.run(dateTraitement, nbThreads, status, idMessage);
	}
}
