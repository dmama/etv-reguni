package ch.vd.uniregctb.identification.contribuable.manager;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresAdresse;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresPersonne;
import ch.vd.uniregctb.evenement.identification.contribuable.Demande;
import ch.vd.uniregctb.evenement.identification.contribuable.Erreur;
import ch.vd.uniregctb.evenement.identification.contribuable.Erreur.TypeErreur;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentCtbDAO;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;
import ch.vd.uniregctb.identification.contribuable.AciComService;
import ch.vd.uniregctb.identification.contribuable.FichierOrigine;
import ch.vd.uniregctb.identification.contribuable.IdentificationContribuableService;
import ch.vd.uniregctb.identification.contribuable.view.DemandeIdentificationView;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesEditView;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeTiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.webservice.acicom.AciComClientException;

public class IdentificationMessagesEditManagerImpl implements IdentificationMessagesEditManager {

	private IdentificationContribuableService identCtbService;

	private IdentCtbDAO identCtbDAO;

	private TiersDAO tiersDAO;

	private AciComService aciComService;

	private final Set<String> viewableTypes = new HashSet<String>();

	@SuppressWarnings({"UnusedDeclaration"})
	public void setIdentCtbService(IdentificationContribuableService identCtbService) {
		this.identCtbService = identCtbService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setIdentCtbDAO(IdentCtbDAO identCtbDAO) {
		this.identCtbDAO = identCtbDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setViewableTypes(String viewableTypesString) {
		if (viewableTypesString != null) {
			final String[] tokens = viewableTypesString.split("[ ,]");
			for (String token : tokens) {
				if (StringUtils.isNotBlank(token)) {
					viewableTypes.add(token.trim());
				}
			}
		}
	}

	/**
	 * Alimente la vue
	 * @param id
	 * @return la vue
	 * @throws Exception
	 */
	@Transactional(readOnly = true)
	public IdentificationMessagesEditView getView(Long id) throws Exception {
		IdentificationMessagesEditView identificationMessagesEditView = new IdentificationMessagesEditView();
		identificationMessagesEditView.setTypeTiers(TypeTiers.PERSONNE_PHYSIQUE);
		identificationMessagesEditView.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.EST_EXACTEMENT);
		identificationMessagesEditView.setDemandeIdentificationView(getDemandeIdentificationView (id));
		IdentificationContribuable identificationContribuable = identCtbDAO.get(id);

		String nomRaison = "";
		if (identificationContribuable.getDemande().getPersonne().getNom() != null) {
			nomRaison = nomRaison + identificationContribuable.getDemande().getPersonne().getNom();
		}
		if (identificationContribuable.getDemande().getPersonne().getPrenoms() != null) {
			if (!"".equals(nomRaison)) {
				nomRaison = nomRaison + " " + identificationContribuable.getDemande().getPersonne().getPrenoms() ;
			}
			else {
				nomRaison = identificationContribuable.getDemande().getPersonne().getPrenoms() ;
			}
		}
		identificationMessagesEditView.setNomRaison(nomRaison);
		identificationMessagesEditView.setNumeroAVS(FormatNumeroHelper.formatNumAVS(identificationContribuable.getDemande().getPersonne().getNAVS13()));
		if("".equals(identificationMessagesEditView.getNumeroAVS())){
			identificationMessagesEditView.setNumeroAVS(FormatNumeroHelper.formatAncienNumAVS(identificationContribuable.getDemande().getPersonne().getNAVS11()));	
		}

		if (identificationContribuable.getDemande().getPersonne().getDateNaissance() != null) {
			identificationMessagesEditView.setDateNaissance(getDateNaissanceFromDemande(identificationContribuable));
		}
		return identificationMessagesEditView;
	}

	/**Permet de retourner la bonne valeur pour la date de naissance
	 * les dates avant le 01.01.1901 et après la date du mesage sont considérées comme vides
	 *
	 * @param identificationContribuable
	 * @return
	 */
	private RegDate getDateNaissanceFromDemande(IdentificationContribuable identificationContribuable) {
		RegDate dateNaissance = identificationContribuable.getDemande().getPersonne().getDateNaissance();
		final RegDate dateAuPlusTot = RegDate.get(1901, 1, 1);
		final RegDate dateAuPlusTard = RegDate.get(identificationContribuable.getDemande().getDate());
		if(dateNaissance != null &&(dateNaissance.isBefore(dateAuPlusTot) || dateNaissance.isAfter(dateAuPlusTard))){
			dateNaissance = null;
		}
		return dateNaissance;  //To change body of created methods use File | Settings | File Templates.
	}

	/**
	 * Alimente le cartouche de demande d'identification
	 * @param id
	 * @return la vue du cartouche
	 * @throws Exception
	 */
	@Transactional(readOnly = true)
	public DemandeIdentificationView getDemandeIdentificationView (Long id) throws Exception {

		final IdentificationContribuable identificationContribuable = identCtbDAO.get(id);
		final DemandeIdentificationView demandeIdentificationView = new DemandeIdentificationView();
		demandeIdentificationView.setId(identificationContribuable.getId());
		demandeIdentificationView.setEtatMessage(identificationContribuable.getEtat());

		final Demande demande = identificationContribuable.getDemande();
		if (demande != null) {
			demandeIdentificationView.setDateMessage(demande.getDate());
			demandeIdentificationView.setEmetteurId(demande.getEmetteurId());
			demandeIdentificationView.setPeriodeFiscale(demande.getPeriodeFiscale());
			demandeIdentificationView.setTypeMessage(demande.getTypeMessage());
			demandeIdentificationView.setBusinessId(identificationContribuable.getHeader().getBusinessId());
			demandeIdentificationView.setViewable(showViewLink(demande.getTypeMessage()));

			final CriteresPersonne personne = demande.getPersonne();
			if (personne != null) {
				demandeIdentificationView.setNom(personne.getNom());
				demandeIdentificationView.setPrenoms(personne.getPrenoms());
				demandeIdentificationView.setNavs13(FormatNumeroHelper.formatNumAVS(personne.getNAVS13()));
				demandeIdentificationView.setNavs11(FormatNumeroHelper.formatAncienNumAVS(personne.getNAVS11()));
				demandeIdentificationView.setDateNaissance(personne.getDateNaissance());
				demandeIdentificationView.setSexe(personne.getSexe());
				demandeIdentificationView.setAnnule(identificationContribuable.isAnnule());

				final CriteresAdresse adresse = personne.getAdresse();
				if (adresse != null) {
					demandeIdentificationView.setRue(adresse.getRue());
					demandeIdentificationView.setNpa(adresse.getNpaSuisse());
					demandeIdentificationView.setLieu(adresse.getLieu());
					demandeIdentificationView.setPays(adresse.getCodePays());
					demandeIdentificationView.setNpaEtranger(adresse.getNpaEtranger());
					demandeIdentificationView.setNoPolice(adresse.getNoPolice());
				}
			}
		}
		return demandeIdentificationView;
	}

	private boolean showViewLink(String typeMessage) {
		return viewableTypes.contains(typeMessage);
	}

	/**
	 * Force l'identification du contribuable
	 * @param idIdentification
	 * @param idPersonne
	 * @throws Exception
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void forceIdentification(Long idIdentification, Long idPersonne, Etat etat) throws Exception {
		IdentificationContribuable identificationContribuable = identCtbDAO.get(idIdentification);
		PersonnePhysique personne = (PersonnePhysique) tiersDAO.get(idPersonne);
		identCtbService.forceIdentification(identificationContribuable, personne, etat);
	}

	/**
	 * Donne à expertiser
	 * @param idIdentification
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void expertiser(Long idIdentification) {
		IdentificationContribuable identificationContribuable = identCtbDAO.get(idIdentification);
		identificationContribuable.setEtat(Etat.A_EXPERTISER);
	}

	/**
	 * Impossible à identifier
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void impossibleAIdentifier(IdentificationMessagesEditView bean) throws Exception {

		Long idIdentification = bean.getDemandeIdentificationView().getId();
		IdentificationContribuable identificationContribuable = identCtbDAO.get(idIdentification );

		Erreur erreur = new Erreur(TypeErreur.METIER, "01", bean.getErreurMessage().getLibelle());
		identCtbService.impossibleAIdentifier(identificationContribuable, erreur);
	}
	@Transactional(rollbackFor = Throwable.class)
	public  void verouillerMessage(Long idIdentification) throws Exception {
		IdentificationContribuable identificationContribuable = identCtbDAO.get(idIdentification);
		String user = AuthenticationHelper.getCurrentPrincipal();
		identificationContribuable.setUtilisateurTraitant(user);

	}

	@Transactional(rollbackFor = Throwable.class)
	public void deVerouillerMessage(Long idIdentification) throws Exception {
		IdentificationContribuable identificationContribuable = identCtbDAO.get(idIdentification);
		String userCourant = AuthenticationHelper.getCurrentPrincipal();
		if (userCourant.equals(identificationContribuable.getUtilisateurTraitant())) {
			identificationContribuable.setUtilisateurTraitant(null);
		}
	}

	public FichierOrigine getMessageFile(String businessId) throws AciComClientException {
		return aciComService.getMessageFile(businessId);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setAciComService(AciComService aciComService) {
		this.aciComService = aciComService;
	}
}
