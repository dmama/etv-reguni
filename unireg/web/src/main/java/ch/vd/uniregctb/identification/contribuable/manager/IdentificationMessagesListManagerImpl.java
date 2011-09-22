package ch.vd.uniregctb.identification.contribuable.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

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
import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;
import ch.vd.uniregctb.identification.contribuable.IdentifiantUtilisateur;
import ch.vd.uniregctb.identification.contribuable.IdentificationContribuableService;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesListView;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesResultView;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;

public class IdentificationMessagesListManagerImpl implements IdentificationMessagesListManager {

	private static final String TOUS = "TOUS";

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
	public IdentificationMessagesListView getView(String parametreTypeMessage, String parametrePeriode, String parametreEtat) {
		IdentificationMessagesListView identificationMessagesListView = new IdentificationMessagesListView();
		identificationMessagesListView.setUserCourant(AuthenticationHelper.getCurrentPrincipal());

		if (parametreTypeMessage == null && parametrePeriode == null && parametreEtat == null) {
			identificationMessagesListView.setTypeMessage(TOUS);
			identificationMessagesListView.setPeriodeFiscale(-1);
			identificationMessagesListView.setPrioriteEmetteur(TOUS);
			identificationMessagesListView.setEtatMessage(TOUS);
		}
		else {
			if (parametreTypeMessage != null) {
				identificationMessagesListView.setTypeMessage(parametreTypeMessage);
			}

			if (parametrePeriode != null) {
				identificationMessagesListView.setPeriodeFiscale(Integer.valueOf(parametrePeriode));
			}

			if (parametreEtat != null) {
				identificationMessagesListView.setEtatMessage(parametreEtat);
			}
		}


		return identificationMessagesListView;
	}


	/**
	 * Recherche des identifications correspondant aux critères
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
	public List<IdentificationMessagesResultView> find(IdentificationContribuableCriteria bean, WebParamPagination pagination, boolean nonTraiteOnly, boolean archiveOnly,
	                                                   boolean nonTraiterAndSuspendu, @Nullable TypeDemande typeDemande)
			throws AdressesResolutionException, ServiceInfrastructureException {
		List<IdentificationMessagesResultView> identificationsView = new ArrayList<IdentificationMessagesResultView>();
		List<IdentificationContribuable> identifications = identCtbService.find(bean, pagination, nonTraiteOnly, archiveOnly, nonTraiterAndSuspendu, typeDemande);
		for (IdentificationContribuable identification : identifications) {
			IdentificationMessagesResultView identificationView = buildView(identification);
			identificationsView.add(identificationView);
		}

		return identificationsView;
	}

	/**
	 * Recherche des identifications correspondant aux critères
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
	public List<IdentificationMessagesResultView> find(IdentificationContribuableCriteria bean, WebParamPagination pagination, boolean nonTraiteOnly, boolean archiveOnly,
	                                                   boolean nonTraiterAndSuspendu) throws AdressesResolutionException, ServiceInfrastructureException {

		return find(bean, pagination, nonTraiteOnly, archiveOnly, nonTraiterAndSuspendu, null);
	}

	/**
	 * Recherche des identifications correspondant seulement à l'état en cours
	 *
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
	public List<IdentificationMessagesResultView> findEncoursSeul(IdentificationContribuableCriteria bean, WebParamPagination pagination, @Nullable TypeDemande typeDemande)
			throws AdressesResolutionException, ServiceInfrastructureException {

		bean.setEtatMessage(Etat.A_TRAITER_MANUELLEMENT.name());
		return find(bean, pagination, false, false, false, typeDemande);
	}

	/**
	 * Recherche des identifications correspondant seulement à l'état en cours
	 *
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
	public List<IdentificationMessagesResultView> findEncoursSeul(IdentificationContribuableCriteria bean, WebParamPagination pagination)
			throws AdressesResolutionException, ServiceInfrastructureException {

		return findEncoursSeul(bean, pagination, null);
	}

	/**
	 * Cherche et compte les identifications correspondant aux criteres
	 *
	 * @param criterion
	 * @param typeDemande
	 * @return
	 */
	@Override
	@Transactional(readOnly = true)
	public int count(IdentificationContribuableCriteria criterion, boolean nonTraiteOnly, boolean archiveOnly, boolean nonTraiterAndSuspendu, @Nullable TypeDemande typeDemande) {
		return identCtbService.count(criterion, nonTraiteOnly, archiveOnly, nonTraiterAndSuspendu, typeDemande);
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
	public int countEnCoursSeul(IdentificationContribuableCriteria criterion, @Nullable TypeDemande typeDemande) {
		criterion.setEtatMessage(Etat.A_TRAITER_MANUELLEMENT.name());
		return identCtbService.count(criterion, false, false, false, typeDemande);
	}


	/**
	 * Cherche et compte les identifications correspondant aux criteres
	 *
	 * @param criterion
	 * @param typeDemande
	 * @return
	 */
	@Override
	@Transactional(readOnly = true)
	public int count(IdentificationContribuableCriteria criterion, boolean nonTraiteOnly, boolean archiveOnly, boolean nonTraiterAndSuspendu) {
		return count(criterion, nonTraiteOnly, archiveOnly, nonTraiterAndSuspendu, null);
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
	public int countEnCoursSeul(IdentificationContribuableCriteria criterion) {
		return countEnCoursSeul(criterion);
	}

	/**
	 * Suspendre l'identification des messages
	 *
	 * @param identificationMessagesListView
	 * @throws ServiceInfrastructureException
	 * @throws EditiqueException
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void suspendreIdentificationMessages(IdentificationMessagesListView identificationMessagesListView) {
		LOGGER.debug("Tab Ids messages:" + Arrays.toString(identificationMessagesListView.getTabIdsMessages()));
		if (identificationMessagesListView.getTabIdsMessages() != null) {
			for (int i = 0; i < identificationMessagesListView.getTabIdsMessages().length; i++) {
				final IdentificationContribuable identificationContribuable = identCtbDAO.get(identificationMessagesListView.getTabIdsMessages()[i]);
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
	 * @param identificationMessagesListView
	 * @throws ServiceInfrastructureException
	 * @throws EditiqueException
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void ResoumettreIdentificationMessages(IdentificationMessagesListView identificationMessagesListView) {
		LOGGER.debug("Tab Ids messages:" + Arrays.toString(identificationMessagesListView.getTabIdsMessages()));
		if (identificationMessagesListView.getTabIdsMessages() != null) {
			for (int i = 0; i < identificationMessagesListView.getTabIdsMessages().length; i++) {
				final IdentificationContribuable identificationContribuable = identCtbDAO.get(identificationMessagesListView.getTabIdsMessages()[i]);
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
	 * @param identificationMessagesListView
	 * @throws ServiceInfrastructureException
	 * @throws EditiqueException
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void soumettreIdentificationMessages(IdentificationMessagesListView identificationMessagesListView) {
		LOGGER.debug("Tab Ids messages:" + Arrays.toString(identificationMessagesListView.getTabIdsMessages()));
		if (identificationMessagesListView.getTabIdsMessages() != null) {
			for (int i = 0; i < identificationMessagesListView.getTabIdsMessages().length; i++) {
				final IdentificationContribuable identificationContribuable = identCtbDAO.get(identificationMessagesListView.getTabIdsMessages()[i]);
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
		IdentificationMessagesResultView identificationMessagesResultView = new IdentificationMessagesResultView();
		identificationMessagesResultView.setId(identification.getId());
		if (identification.getAnnulationDate() == null) {
			identificationMessagesResultView.setAnnule(false);
		}
		else {
			identificationMessagesResultView.setAnnule(true);
		}
		if (identification.getUtilisateurTraitant() != null) {
			identificationMessagesResultView.setUtilisateurTraitant(identification.getUtilisateurTraitant());
		}

		if (identification.getTraitementUser() != null) {
			IdentifiantUtilisateur identifiantUtilisateur = identCtbService.getNomUtilisateurFromVisaUser(identification.getTraitementUser());
			identificationMessagesResultView.setTraitementUser(identifiantUtilisateur.getNomComplet());
		}

		if (identification.getDateTraitement() != null) {
			identificationMessagesResultView.setTraitementDate(identification.getDateTraitement());
		}
		identificationMessagesResultView.setEtatMessage(identification.getEtat());
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
			Long noContribuable = identification.getReponse().getNoContribuable();
			if (noContribuable != null) {
				identificationMessagesResultView.setNumeroContribuable(noContribuable);
			}
			Erreur erreur = identification.getReponse().getErreur();
			if (erreur != null) {
				identificationMessagesResultView.setMessageRetour(erreur.getMessage());
			}
		}
		return identificationMessagesResultView;
	}


}
