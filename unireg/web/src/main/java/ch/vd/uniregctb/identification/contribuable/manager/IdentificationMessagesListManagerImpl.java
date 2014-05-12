package ch.vd.uniregctb.identification.contribuable.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.evenement.identification.contribuable.Erreur;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentCtbDAO;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableCriteria;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableEtatFilter;
import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;
import ch.vd.uniregctb.identification.contribuable.IdentifiantUtilisateur;
import ch.vd.uniregctb.identification.contribuable.IdentificationContribuableService;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationContribuableListCriteria;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesResultView;

public class IdentificationMessagesListManagerImpl implements IdentificationMessagesListManager {

	protected static final Logger LOGGER = Logger.getLogger(IdentificationMessagesListManagerImpl.class);

	private IdentCtbDAO identCtbDAO;

	private IdentificationContribuableService identCtbService;


	public void setIdentCtbDAO(IdentCtbDAO identCtbDAO) {
		this.identCtbDAO = identCtbDAO;
	}

	public void setIdentCtbService(IdentificationContribuableService identCtbService) {
		this.identCtbService = identCtbService;
	}

	/**
	 * Alimente la vue
	 *
	 * @return
	 */
	@Override
	public IdentificationContribuableListCriteria getView(String parametreTypeMessage, Integer parametrePeriode, Etat parametreEtat) {
		final IdentificationContribuableListCriteria identificationContribuableListCriteria = new IdentificationContribuableListCriteria();
		identificationContribuableListCriteria.setUserCourant(AuthenticationHelper.getCurrentPrincipal());
		identificationContribuableListCriteria.setPrioriteEmetteur(null);
		identificationContribuableListCriteria.setTypeMessage(parametreTypeMessage);
		identificationContribuableListCriteria.setPeriodeFiscale(parametrePeriode);
		identificationContribuableListCriteria.setEtatMessage(parametreEtat);

		return identificationContribuableListCriteria;
	}


	/**
	 * Recherche des identifications correspondant aux critères
	 *
	 *
	 *
	 * @param bean
	 * @param pagination
	 * @param filter
	 *@param typeDemande  @return
	 * @throws AdressesResolutionException
	 * @throws ServiceInfrastructureException
	 */
	@Override
	@Transactional(readOnly = true)
	public List<IdentificationMessagesResultView> find(IdentificationContribuableCriteria bean, WebParamPagination pagination,
	                                                   IdentificationContribuableEtatFilter filter, TypeDemande... typeDemande) throws AdressesResolutionException, ServiceInfrastructureException {
		final List<IdentificationMessagesResultView> identificationsView = new ArrayList<>();
		final List<IdentificationContribuable> identifications = identCtbService.find(bean, pagination, filter, typeDemande);
		for (IdentificationContribuable identification : identifications) {
			IdentificationMessagesResultView identificationView = buildView(identification);
			identificationsView.add(identificationView);
		}

		return identificationsView;
	}

	/**
	 * Recherche des identifications correspondant seulement à l'état en cours
	 *
	 * @param bean
	 * @param pagination
	 * @param typeDemande
	 * @return
	 * @throws AdressesResolutionException
	 * @throws ServiceInfrastructureException
	 */
	@Override
	@Transactional(readOnly = true)
	public List<IdentificationMessagesResultView> findEncoursSeul(IdentificationContribuableCriteria bean, WebParamPagination pagination, TypeDemande... typeDemande)
			throws AdressesResolutionException, ServiceInfrastructureException {

		bean.setEtatMessage(Etat.A_TRAITER_MANUELLEMENT);
		return find(bean, pagination, IdentificationContribuableEtatFilter.SEULEMENT_A_TRAITER_MANUELLEMENT, typeDemande);
	}

	/**
	 * Cherche et compte les identifications correspondant aux criteres
	 *
	 *
	 * @param criterion
	 * @param typeDemande
	 * @return
	 */
	@Override
	@Transactional(readOnly = true)
	public int count(IdentificationContribuableCriteria criterion, IdentificationContribuableEtatFilter filter, TypeDemande... typeDemande) {
		return identCtbService.count(criterion, filter, typeDemande);
	}

	/**
	 * Cherche et compte les identifications correspondant à l'etat en cours
	 *
	 * @param criterion
	 * @param typeDemande
	 * @return
	 */
	@Override
	@Transactional(readOnly = true)
	public int countEnCoursSeul(IdentificationContribuableCriteria criterion, TypeDemande... typeDemande) {
		criterion.setEtatMessage(Etat.A_TRAITER_MANUELLEMENT);
		return identCtbService.count(criterion, IdentificationContribuableEtatFilter.SEULEMENT_A_TRAITER_MANUELLEMENT, typeDemande);
	}


	/**
	 * Suspendre l'identification des messages
	 *
	 * @param identificationContribuableListCriteria
	 * @throws ServiceInfrastructureException
	 * @throws EditiqueException
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void suspendreIdentificationMessages(IdentificationContribuableListCriteria identificationContribuableListCriteria) {
		LOGGER.debug("Tab Ids messages:" + Arrays.toString(identificationContribuableListCriteria.getTabIdsMessages()));
		if (identificationContribuableListCriteria.getTabIdsMessages() != null) {
			for (int i = 0; i < identificationContribuableListCriteria.getTabIdsMessages().length; i++) {
				final IdentificationContribuable identificationContribuable = identCtbDAO.get(identificationContribuableListCriteria.getTabIdsMessages()[i]);
				if (Etat.A_TRAITER_MANUELLEMENT == identificationContribuable.getEtat()) {
					identificationContribuable.setEtat(Etat.A_TRAITER_MAN_SUSPENDU);
				}
				else if (Etat.A_EXPERTISER == identificationContribuable.getEtat()) {
					identificationContribuable.setEtat(Etat.A_EXPERTISER_SUSPENDU);
				}
			}
		}
	}


	/**
	 * Re soumettre l'identification des messages qui sont remis "dans le circuit" afin d'être identifié manuellement ou expèrtisé
	 *
	 *
	 * @param identificationContribuableListCriteria
	 * @throws ServiceInfrastructureException
	 * @throws EditiqueException
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void reSoumettreIdentificationMessages(IdentificationContribuableListCriteria identificationContribuableListCriteria) {
		LOGGER.debug("Tab Ids messages:" + Arrays.toString(identificationContribuableListCriteria.getTabIdsMessages()));
		if (identificationContribuableListCriteria.getTabIdsMessages() != null) {
			for (int i = 0; i < identificationContribuableListCriteria.getTabIdsMessages().length; i++) {
				final IdentificationContribuable identificationContribuable = identCtbDAO.get(identificationContribuableListCriteria.getTabIdsMessages()[i]);
				if (Etat.A_TRAITER_MAN_SUSPENDU == identificationContribuable.getEtat()) {
					identificationContribuable.setEtat(Etat.A_TRAITER_MANUELLEMENT);
				}
				else if (Etat.A_EXPERTISER_SUSPENDU == identificationContribuable.getEtat()) {
					identificationContribuable.setEtat(Etat.A_EXPERTISER);
				}
			}
		}
	}

	/**
	 * Soumettre l'identification des messages
	 *
	 * @param identificationContribuableListCriteria
	 * @throws ServiceInfrastructureException
	 * @throws EditiqueException
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void soumettreIdentificationMessages(IdentificationContribuableListCriteria identificationContribuableListCriteria) {
		LOGGER.debug("Tab Ids messages:" + Arrays.toString(identificationContribuableListCriteria.getTabIdsMessages()));
		if (identificationContribuableListCriteria.getTabIdsMessages() != null) {
			for (int i = 0; i < identificationContribuableListCriteria.getTabIdsMessages().length; i++) {
				final IdentificationContribuable identificationContribuable = identCtbDAO.get(identificationContribuableListCriteria.getTabIdsMessages()[i]);
				identCtbService.soumettre(identificationContribuable);
			}
		}
	}

	/**
	 * Construit la vue
	 *
	 * @param identification
	 * @return
	 * @throws ServiceInfrastructureException
	 */
	private IdentificationMessagesResultView buildView(IdentificationContribuable identification) throws ServiceInfrastructureException {
		final IdentificationMessagesResultView identificationMessagesResultView = new IdentificationMessagesResultView();
		identificationMessagesResultView.setId(identification.getId());
		identificationMessagesResultView.setAnnule(identification.isAnnule());
		identificationMessagesResultView.setUtilisateurTraitant(identification.getUtilisateurTraitant());

		if (identification.getTraitementUser() != null) {
			final IdentifiantUtilisateur identifiantUtilisateur = identCtbService.getNomUtilisateurFromVisaUser(identification.getTraitementUser());
			identificationMessagesResultView.setTraitementUser(identifiantUtilisateur.getNomComplet());
		}

		identificationMessagesResultView.setTraitementDate(identification.getDateTraitement());
		identificationMessagesResultView.setEtatMessage(identification.getEtat());
		if (identification.getNAVS13Upi() != null) {
			identificationMessagesResultView.setNavs13Upi(FormatNumeroHelper.formatNumAVS(identification.getNAVS13Upi()));
		}

		if (identification.getDemande() != null) {
			identificationMessagesResultView.setDateMessage(identification.getDemande().getDate());
			identificationMessagesResultView.setEmetteurId(identification.getDemande().getEmetteurId());
			identificationMessagesResultView.setPeriodeFiscale(identification.getDemande().getPeriodeFiscale());
			identificationMessagesResultView.setTypeMessage(identification.getDemande().getTypeMessage());
			identificationMessagesResultView.setTransmetteur(identification.getDemande().getTransmetteur());
			identificationMessagesResultView.setMontant(identification.getDemande().getMontant());
			if (identification.getDemande().getPersonne() != null) {
				identificationMessagesResultView.setNavs13(FormatNumeroHelper.formatNumAVS(identification.getDemande().getPersonne().getNAVS13()));
				identificationMessagesResultView.setNavs11(FormatNumeroHelper.formatAncienNumAVS(identification.getDemande().getPersonne().getNAVS11()));
				identificationMessagesResultView.setNom(identification.getDemande().getPersonne().getNom());
				identificationMessagesResultView.setPrenoms(identification.getDemande().getPersonne().getPrenoms());
				identificationMessagesResultView.setDateNaissance(identification.getDemande().getPersonne().getDateNaissance());

			}
		}

		if (identification.getReponse() != null) {
			//Message par Défaut
			identificationMessagesResultView.setMessageRetour("Identifié");
			final Long noContribuable = identification.getReponse().getNoContribuable();
			if (noContribuable != null) {
				identificationMessagesResultView.setNumeroContribuable(noContribuable);
			}
			final Erreur erreur = identification.getReponse().getErreur();
			if (erreur != null) {
				identificationMessagesResultView.setMessageRetour(erreur.getMessage());
			}
		}
		return identificationMessagesResultView;
	}


}
