package ch.vd.uniregctb.identification.contribuable.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresAdresse;
import ch.vd.uniregctb.evenement.identification.contribuable.Erreur;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentCtbDAO;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.evenement.identification.contribuable.Erreur.TypeErreur;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;
import ch.vd.uniregctb.identification.contribuable.AciComService;
import ch.vd.uniregctb.identification.contribuable.FichierOrigine;
import ch.vd.uniregctb.identification.contribuable.IdentificationContribuableService;
import ch.vd.uniregctb.identification.contribuable.view.DemandeIdentificationView;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesEditView;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeTiers;
import ch.vd.uniregctb.webservice.acicom.AciComClientException;

public class IdentificationMessagesEditManagerImpl implements IdentificationMessagesEditManager {

	private IdentificationContribuableService identCtbService;

	private IdentCtbDAO identCtbDAO;

	private TiersDAO tiersDAO;

	private AciComService aciComService;

	public void setIdentCtbService(IdentificationContribuableService identCtbService) {
		this.identCtbService = identCtbService;
	}

	public void setIdentCtbDAO(IdentCtbDAO identCtbDAO) {
		this.identCtbDAO = identCtbDAO;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
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
		identificationMessagesEditView.setTypeRechercheDuNom(IdentificationMessagesEditView.TypeRecherche.EST_EXACTEMENT);
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
		if (identificationContribuable.getDemande().getPersonne().getDateNaissance() != null) {
			identificationMessagesEditView.setDateNaissance(identificationContribuable.getDemande().getPersonne().getDateNaissance());
		}
		return identificationMessagesEditView;
	}

	/**
	 * Alimente le cartouche de demande d'identification
	 * @param id
	 * @return la vue du cartouche
	 * @throws Exception
	 */
	@Transactional(readOnly = true)
	public DemandeIdentificationView getDemandeIdentificationView (Long id) throws Exception {

		IdentificationContribuable identificationContribuable = identCtbDAO.get(id);
		DemandeIdentificationView demandeIdentificationView = new DemandeIdentificationView();
		demandeIdentificationView.setId(identificationContribuable.getId());
		demandeIdentificationView.setEtatMessage(identificationContribuable.getEtat());
		if(identificationContribuable.getDemande() != null) {
			demandeIdentificationView.setDateMessage(identificationContribuable.getDemande().getDate());
			demandeIdentificationView.setEmetteurId(identCtbService.getNomCantonFromEmetteurId(identificationContribuable.getDemande().getEmetteurId()));
			demandeIdentificationView.setPeriodeFiscale(Integer.valueOf(identificationContribuable.getDemande().getPeriodeFiscale()));
			demandeIdentificationView.setTypeMessage(identificationContribuable.getDemande().getTypeMessage());
			demandeIdentificationView.setBusinessId(identificationContribuable.getHeader().getBusinessId());
			if(identificationContribuable.getDemande().getPersonne() != null) {
				demandeIdentificationView.setNom(identificationContribuable.getDemande().getPersonne().getNom());
				demandeIdentificationView.setPrenoms(identificationContribuable.getDemande().getPersonne().getPrenoms());
				demandeIdentificationView.setNavs13(FormatNumeroHelper.formatNumAVS(identificationContribuable.getDemande().getPersonne().getNAVS13()));
				demandeIdentificationView.setDateNaissance(identificationContribuable.getDemande().getPersonne().getDateNaissance());
				demandeIdentificationView.setSexe(identificationContribuable.getDemande().getPersonne().getSexe());
				demandeIdentificationView.setAnnule(identificationContribuable.isAnnule());
				if (identificationContribuable.getDemande().getPersonne().getAdresse()!=null) {
					CriteresAdresse adresse = identificationContribuable.getDemande().getPersonne().getAdresse();
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

	public AciComService getAciComService() {
		return aciComService;
	}

	public void setAciComService(AciComService aciComService) {
		this.aciComService = aciComService;
	}
}
