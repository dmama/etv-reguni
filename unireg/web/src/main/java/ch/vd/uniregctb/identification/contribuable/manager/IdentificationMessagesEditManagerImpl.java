package ch.vd.uniregctb.identification.contribuable.manager;

import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.annotation.Transactional;

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
import ch.vd.uniregctb.identification.contribuable.IdentificationContribuableService;
import ch.vd.uniregctb.identification.contribuable.view.DemandeIdentificationView;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesEditView;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeTiers;
import ch.vd.uniregctb.tiers.TiersDAO;

public class IdentificationMessagesEditManagerImpl implements IdentificationMessagesEditManager {

	private IdentificationContribuableService identCtbService;

	private IdentCtbDAO identCtbDAO;

	private TiersDAO tiersDAO;

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

	/**
	 * Alimente la vue
	 * @param id
	 * @return la vue
	 * @throws Exception
	 */
	@Override
	@Transactional(readOnly = true)
	public IdentificationMessagesEditView getView(Long id) throws Exception {
		final IdentificationMessagesEditView identificationMessagesEditView = new IdentificationMessagesEditView();
		identificationMessagesEditView.setTypeTiers(TypeTiers.PERSONNE_PHYSIQUE);
		identificationMessagesEditView.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.EST_EXACTEMENT);
		identificationMessagesEditView.setDemandeIdentificationView(getDemandeIdentificationView(id));
		final IdentificationContribuable identificationContribuable = identCtbDAO.get(id);

		String nomRaison = "";
		final CriteresPersonne personne = identificationContribuable.getDemande().getPersonne();
		if (personne != null) {
			if (personne.getNom() != null) {
				nomRaison = nomRaison + personne.getNom();
			}
			if (personne.getPrenoms() != null) {
				if (!"".equals(nomRaison)) {
					nomRaison = nomRaison + ' ' + personne.getPrenoms();
				}
				else {
					nomRaison = personne.getPrenoms();
				}
			}
			identificationMessagesEditView.setNomRaison(nomRaison);
			identificationMessagesEditView.setNumeroAVS(FormatNumeroHelper.formatNumAVS(personne.getNAVS13()));
			if ("".equals(identificationMessagesEditView.getNumeroAVS())) {
				identificationMessagesEditView.setNumeroAVS(FormatNumeroHelper.formatAncienNumAVS(personne.getNAVS11()));
			}
		}
		return identificationMessagesEditView;
	}

	/**
	 * Alimente le cartouche de demande d'identification
	 * @param id
	 * @return la vue du cartouche
	 * @throws Exception
	 */
	@Override
	@Transactional(readOnly = true)
	public DemandeIdentificationView getDemandeIdentificationView(Long id) throws Exception {

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
			demandeIdentificationView.setDocumentUrl(getDocumentUrl(identificationContribuable));
			demandeIdentificationView.setTransmetteur(demande.getTransmetteur());
			demandeIdentificationView.setMontant(demande.getMontant());

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

	private static String getDocumentUrl(IdentificationContribuable msg) {
		final String documentUrl = msg.getHeader().getDocumentUrl();
		return StringUtils.isNotBlank(documentUrl) ? documentUrl : null;
	}

	/**
	 * Force l'identification du contribuable
	 * @param idIdentification
	 * @param idPersonne
	 * @throws Exception
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void forceIdentification(Long idIdentification, Long idPersonne, Etat etat) throws Exception {
		final IdentificationContribuable identificationContribuable = identCtbDAO.get(idIdentification);
		final PersonnePhysique personne = (PersonnePhysique) tiersDAO.get(idPersonne);
		identCtbService.forceIdentification(identificationContribuable, personne, etat);
	}

	/**
	 * Donne à expertiser
	 * @param idIdentification
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void expertiser(Long idIdentification) {
		final IdentificationContribuable identificationContribuable = identCtbDAO.get(idIdentification);
		identificationContribuable.setEtat(Etat.A_EXPERTISER);
	}

	/**
	 * Impossible à identifier
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void impossibleAIdentifier(IdentificationMessagesEditView bean) throws Exception {

		final Long idIdentification = bean.getDemandeIdentificationView().getId();
		final IdentificationContribuable identificationContribuable = identCtbDAO.get(idIdentification);

		final Erreur erreur = new Erreur(TypeErreur.METIER, bean.getErreurMessage().getCode(), bean.getErreurMessage().getLibelle());
		identCtbService.impossibleAIdentifier(identificationContribuable, erreur);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void verouillerMessage(Long idIdentification) throws Exception {
		final IdentificationContribuable identificationContribuable = identCtbDAO.get(idIdentification);
		final String user = AuthenticationHelper.getCurrentPrincipal();
		identificationContribuable.setUtilisateurTraitant(user);

	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void deVerouillerMessage(Long idIdentification) throws Exception {
		final IdentificationContribuable identificationContribuable = identCtbDAO.get(idIdentification);
		final String userCourant = AuthenticationHelper.getCurrentPrincipal();
		if (userCourant.equals(identificationContribuable.getUtilisateurTraitant())) {
			identificationContribuable.setUtilisateurTraitant(null);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isMessageVerouille(Long idIdentification) throws Exception {
		final IdentificationContribuable identificationContribuable = identCtbDAO.get(idIdentification);
		final String utilisateurTraitant = identificationContribuable.getUtilisateurTraitant();
		final String user = AuthenticationHelper.getCurrentPrincipal();
		if(utilisateurTraitant !=null && !user.equals(utilisateurTraitant)){
			return true;
		}else{
			return false;
		}

	}
}
