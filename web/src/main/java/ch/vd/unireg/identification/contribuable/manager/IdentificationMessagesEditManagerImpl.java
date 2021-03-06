package ch.vd.unireg.identification.contribuable.manager;

import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.evenement.identification.contribuable.CriteresAdresse;
import ch.vd.unireg.evenement.identification.contribuable.CriteresPersonne;
import ch.vd.unireg.evenement.identification.contribuable.Demande;
import ch.vd.unireg.evenement.identification.contribuable.Erreur;
import ch.vd.unireg.evenement.identification.contribuable.Erreur.TypeErreur;
import ch.vd.unireg.evenement.identification.contribuable.IdentCtbDAO;
import ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuable.Etat;
import ch.vd.unireg.identification.contribuable.IdentificationContribuableService;
import ch.vd.unireg.identification.contribuable.view.DemandeIdentificationView;
import ch.vd.unireg.identification.contribuable.view.IdentificationMessagesEditView;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersCriteria.TypeTiers;
import ch.vd.unireg.tiers.TiersDAO;

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
	 * @return la vue
	 */
	@Override
	@Transactional(readOnly = true)
	public IdentificationMessagesEditView getView(Long id) {
		final IdentificationMessagesEditView identificationMessagesEditView = new IdentificationMessagesEditView();
		identificationMessagesEditView.setDemandeIdentificationView(getDemandeIdentificationView(id));
		final IdentificationContribuable identificationContribuable = identCtbDAO.get(id);
		if (identificationContribuable == null) {
			throw new ObjectNotFoundException("L'identification avec l'id=" + id + " n'existe pas.");
		}
		if (identificationContribuable.getReponse() != null) {
			identificationMessagesEditView.setNoCtbIdentifie(identificationContribuable.getReponse().getNoContribuable());
		}
		return identificationMessagesEditView;
	}

	/**
	 * Alimente le cartouche de demande d'identification
	 * @return la vue du cartouche
	 */
	@Override
	@Transactional(readOnly = true)
	public DemandeIdentificationView getDemandeIdentificationView(Long id) {

		final IdentificationContribuable identificationContribuable = identCtbDAO.get(id);
		if (identificationContribuable == null) {
			throw new ObjectNotFoundException("L'identification avec l'id=" + id + " n'existe pas.");
		}
		final DemandeIdentificationView demandeIdentificationView = new DemandeIdentificationView();
		demandeIdentificationView.setId(identificationContribuable.getId());
		demandeIdentificationView.setEtatMessage(identificationContribuable.getEtat());
		demandeIdentificationView.setCommentaireTraitement(identificationContribuable.getCommentaireTraitement());
		demandeIdentificationView.setNavs13Upi(StringUtils.trimToNull(FormatNumeroHelper.formatNumAVS(identificationContribuable.getNAVS13Upi())));

		final Demande demande = identificationContribuable.getDemande();
		if (demande != null) {
			demandeIdentificationView.setDateMessage(demande.getDate());
			demandeIdentificationView.setEmetteurId(demande.getEmetteurId());
			demandeIdentificationView.setPeriodeFiscale(demande.getPeriodeFiscale());
			demandeIdentificationView.setTypeMessage(demande.getTypeMessage());
			demandeIdentificationView.setTypeDemande(demande.getTypeDemande());
			demandeIdentificationView.setBusinessId(identificationContribuable.getHeader().getBusinessId());
			demandeIdentificationView.setDocumentUrl(getDocumentUrl(identificationContribuable));
			demandeIdentificationView.setTransmetteur(demande.getTransmetteur());
			demandeIdentificationView.setMontant(demande.getMontant());
			demandeIdentificationView.setTypeContribuable(TypeTiers.fromCore(demande.getTypeContribuableRecherche()));

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
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void forceIdentification(Long idIdentification, Long idPersonne, Etat etat) {
		final IdentificationContribuable identificationContribuable = identCtbDAO.get(idIdentification);
		if (identificationContribuable == null) {
			throw new ObjectNotFoundException("L'identification avec l'id=" + idIdentification + " n'existe pas.");
		}
		final Tiers tiers = tiersDAO.get(idPersonne);
		if (tiers instanceof PersonnePhysique) {
			identCtbService.forceIdentification(identificationContribuable, (PersonnePhysique) tiers, etat);
		}
		else if (tiers instanceof Entreprise) {
			identCtbService.forceIdentification(identificationContribuable, (Entreprise) tiers, etat);
		}
		else {
			throw new IllegalArgumentException("Type de tiers invalide : " + tiers);
		}
	}

	/**
	 * Donne à expertiser
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void expertiser(Long idIdentification) {
		final IdentificationContribuable identificationContribuable = identCtbDAO.get(idIdentification);
		if (identificationContribuable == null) {
			throw new ObjectNotFoundException("L'identification avec l'id=" + idIdentification + " n'existe pas.");
		}
		identificationContribuable.setEtat(Etat.A_EXPERTISER);
	}

	/**
	 * Impossible à identifier
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void impossibleAIdentifier(Long idIdentification, IdentificationContribuable.ErreurMessage message) {
		final IdentificationContribuable identificationContribuable = identCtbDAO.get(idIdentification);
		if (identificationContribuable == null) {
			throw new ObjectNotFoundException("L'identification avec l'id=" + idIdentification + " n'existe pas.");
		}
		final Erreur erreur = new Erreur(TypeErreur.METIER, message.getCode(), message.getLibelle());
		identCtbService.impossibleAIdentifier(identificationContribuable, erreur);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void verouillerMessage(Long idIdentification) {
		final IdentificationContribuable identificationContribuable = identCtbDAO.get(idIdentification);
		if (identificationContribuable == null) {
			throw new ObjectNotFoundException("L'identification avec l'id=" + idIdentification + " n'existe pas.");
		}
		final String user = AuthenticationHelper.getCurrentPrincipal();
		identificationContribuable.setUtilisateurTraitant(user);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void deVerouillerMessage(Long idIdentification, boolean byAdmin) {
		final IdentificationContribuable identificationContribuable = identCtbDAO.get(idIdentification);
		if (identificationContribuable == null) {
			throw new ObjectNotFoundException("L'identification avec l'id=" + idIdentification + " n'existe pas.");
		}
		final String userCourant = AuthenticationHelper.getCurrentPrincipal();
		if (userCourant.equals(identificationContribuable.getUtilisateurTraitant()) || byAdmin) {
			identificationContribuable.setUtilisateurTraitant(null);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isMessageVerouille(Long idIdentification) {
		final IdentificationContribuable identificationContribuable = identCtbDAO.get(idIdentification);
		if (identificationContribuable == null) {
			throw new ObjectNotFoundException("L'identification avec l'id=" + idIdentification + " n'existe pas.");
		}
		final String utilisateurTraitant = identificationContribuable.getUtilisateurTraitant();
		final String user = AuthenticationHelper.getCurrentPrincipal();
		return utilisateurTraitant != null && !user.equals(utilisateurTraitant);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public Long relanceIdentificationAuto(long idIdentification) {
		final IdentificationContribuable identificationContribuable = identCtbDAO.get(idIdentification);
		if (identificationContribuable == null) {
			throw new ObjectNotFoundException("L'identification avec l'id=" + idIdentification + " n'existe pas.");
		}
		if (identificationContribuable.getEtat().isEncoreATraiter()) {
			// on ne tente une relance que si l'état n'est pas déjà terminal
			if (identCtbService.tenterIdentificationAutomatiqueContribuable(identificationContribuable)) {
				return identificationContribuable.getReponse().getNoContribuable();
			}
		}
		return null;
	}
}
