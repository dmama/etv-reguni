package ch.vd.uniregctb.identification.contribuable.manager;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentCtbDAO;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableCriteria;
import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;
import ch.vd.uniregctb.identification.contribuable.IdentificationContribuableService;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesListView;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesResultView;

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
	public IdentificationMessagesListView getView(String parametreTypeMessage, String parametrePeriode, String parametreEtat) {
		IdentificationMessagesListView identificationMessagesListView = new IdentificationMessagesListView();
		identificationMessagesListView.setUserCourant(AuthenticationHelper.getCurrentPrincipal());

		if (parametreTypeMessage == null && parametrePeriode == null && parametreEtat == null) {
			identificationMessagesListView.setTypeMessage(TOUS);
			identificationMessagesListView.setPeriodeFiscale(Integer.valueOf(-1));
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
	 * @throws InfrastructureException
	 */
	@Transactional(readOnly = true)
	public List<IdentificationMessagesResultView> find(IdentificationContribuableCriteria bean, WebParamPagination pagination, boolean nonTraiteOnly, boolean archiveOnly,
	                                                boolean nonTraiterAndSuspendu, TypeDemande typeDemande)
			throws AdressesResolutionException, InfrastructureException {
		List<IdentificationMessagesResultView> identificationsView = new ArrayList<IdentificationMessagesResultView>();
		List<IdentificationContribuable> identifications = identCtbService.find(bean, pagination, nonTraiteOnly, archiveOnly, nonTraiterAndSuspendu, typeDemande);
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
	 * @return
	 * @throws AdressesResolutionException
	 * @throws InfrastructureException
	 */
	@Transactional(readOnly = true)
	public List<IdentificationMessagesResultView> findEncoursSeul(IdentificationContribuableCriteria bean, WebParamPagination pagination)
			throws AdressesResolutionException, InfrastructureException {

		bean.setEtatMessage(Etat.A_TRAITER_MANUELLEMENT.name());
		return find(bean, pagination, false, false, false, TypeDemande.MELDEWESEN);
	}

	/**
	 * Cherche et compte les identifications correspondant aux criteres
	 *
	 * @param criterion
	 * @param typeDemande
	 * @return
	 */
	@Transactional(readOnly = true)
	public int count(IdentificationContribuableCriteria criterion, boolean nonTraiteOnly, boolean archiveOnly, boolean nonTraiterAndSuspendu, TypeDemande typeDemande) {
		return identCtbService.count(criterion, nonTraiteOnly, archiveOnly, nonTraiterAndSuspendu, typeDemande);
	}

	/**
	 * Cherche et compte les identifications correspondant à l'etat en cours
	 *
	 * @param criterion
	 * @param typeDemande
	 * @return
	 */
	@Transactional(readOnly = true)
	public int countEnCoursSeul(IdentificationContribuableCriteria criterion, TypeDemande typeDemande) {
		criterion.setEtatMessage(Etat.A_TRAITER_MANUELLEMENT.name());
		return identCtbService.count(criterion, false, false, false, typeDemande);
	}

	/**
	 * Suspendre l'identification des messages
	 *
	 * @param identificationMessagesListView
	 * @throws InfrastructureException
	 * @throws EditiqueException
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void suspendreIdentificationMessages(IdentificationMessagesListView identificationMessagesListView) {
		LOGGER.debug("Tab Ids messages:" + identificationMessagesListView.getTabIdsMessages());
		if (identificationMessagesListView.getTabIdsMessages() != null) {
			for (int i = 0; i < identificationMessagesListView.getTabIdsMessages().length; i++) {
				IdentificationContribuable identificationContribuable = identCtbDAO.get(new Long(identificationMessagesListView.getTabIdsMessages()[i]));
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
	 * @throws InfrastructureException
	 * @throws EditiqueException
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void ResoumettreIdentificationMessages(IdentificationMessagesListView identificationMessagesListView) {
		LOGGER.debug("Tab Ids messages:" + identificationMessagesListView.getTabIdsMessages());
		if (identificationMessagesListView.getTabIdsMessages() != null) {
			for (int i = 0; i < identificationMessagesListView.getTabIdsMessages().length; i++) {
				IdentificationContribuable identificationContribuable = identCtbDAO.get(new Long(identificationMessagesListView.getTabIdsMessages()[i]));
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
	 * @throws InfrastructureException
	 * @throws EditiqueException
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void soumettreIdentificationMessages(IdentificationMessagesListView identificationMessagesListView) {
		LOGGER.debug("Tab Ids messages:" + identificationMessagesListView.getTabIdsMessages());
		if (identificationMessagesListView.getTabIdsMessages() != null) {
			for (int i = 0; i < identificationMessagesListView.getTabIdsMessages().length; i++) {
				IdentificationContribuable identificationContribuable = identCtbDAO.get(new Long(identificationMessagesListView.getTabIdsMessages()[i]));
				identCtbService.soumettre(identificationContribuable);
			}
		}
	}

	/**
	 * Construit la vue
	 *
	 * @param identification
	 * @return
	 * @throws InfrastructureException
	 */
	private IdentificationMessagesResultView buildView(IdentificationContribuable identification) throws InfrastructureException {
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

		if(identification.getTraitementUser()!=null){
			List<String> noms = identCtbService.getNomUtilisateurFromVisaUser(identification.getTraitementUser());
			identificationMessagesResultView.setTraitementUser(noms.get(1));
		}

		if(identification.getTraitementDate()!=null){
			identificationMessagesResultView.setTraitementDate(identification.getTraitementDate());
		}
		identificationMessagesResultView.setEtatMessage(identification.getEtat());
		if (identification.getDemande() != null) {
			identificationMessagesResultView.setDateMessage(identification.getDemande().getDate());
			identificationMessagesResultView.setEmetteurId(identCtbService.getNomCantonFromEmetteurId(identification.getDemande().getEmetteurId()));
			identificationMessagesResultView.setPeriodeFiscale(Integer.valueOf(identification.getDemande().getPeriodeFiscale()));
			identificationMessagesResultView.setTypeMessage(identification.getDemande().getTypeMessage());
			if (identification.getDemande().getPersonne() != null) {
				identificationMessagesResultView.setNavs13(FormatNumeroHelper.formatNumAVS(identification.getDemande().getPersonne().getNAVS13()));
				identificationMessagesResultView.setNavs11(FormatNumeroHelper.formatAncienNumAVS(identification.getDemande().getPersonne().getNAVS11()));
				identificationMessagesResultView.setNom(identification.getDemande().getPersonne().getNom());
				identificationMessagesResultView.setPrenoms(identification.getDemande().getPersonne().getPrenoms());
				identificationMessagesResultView.setDateNaissance(identification.getDemande().getPersonne().getDateNaissance());

			}
		}

		if (identification.getReponse() != null) {
			Long noContribuable = identification.getReponse().getNoContribuable();
			if (noContribuable != null) {
				identificationMessagesResultView.setNumeroContribuable(noContribuable);
			}

		}
		return identificationMessagesResultView;
	}


}
