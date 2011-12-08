package ch.vd.uniregctb.identification.contribuable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.evenement.identification.contribuable.Demande;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;
import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;
import ch.vd.uniregctb.identification.contribuable.manager.IdentificationMessagesListManager;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesListView;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesResultView;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;

public class IdentificationMessagesListEnCoursController extends AbstractIdentificationController {

	protected static final Logger LOGGER = Logger.getLogger(IdentificationMessagesListEnCoursController.class);


	public final static String BOUTON_SUSPENDRE = "suspendre";
	public final static String BOUTON_SOUMETTRE = "soumettre";


	private static final String ACTION_PARAMETER_NAME = "action";
	private static final String EFFACER_PARAMETER_VALUE = "effacer";
	public static final String IDENTIFICATION_CRITERIA_NAME = "identificationCriteria";
	public static final String RESULT_SIZE_NAME = "resultSize";
	public static final Integer PAGE_SIZE = 25;
	public static final String IDENTIFICATION_LIST_ATTRIBUTE_NAME = "identifications";
	public static final String IDENTIFICATION_LIST_ATTRIBUTE_SIZE = "identificationsSize";
	private static final String TABLE_IDENTIFICATION_ID = "message";


	private IdentificationMessagesListManager identificationMessagesListManager;

	public void setIdentificationMessagesListManager(IdentificationMessagesListManager identificationMessagesListManager) {
		this.identificationMessagesListManager = identificationMessagesListManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {


		LOGGER.debug("Start of IdentificationMessagesListEnCoursController:formBackingObject");

		// on doit avoir les droits de visualisation pour ça...
		if (!SecurityProvider.isAnyGranted(Role.MW_IDENT_CTB_VISU, Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_CELLULE_BO, Role.MW_IDENT_CTB_GEST_BO, Role.NCS_IDENT_CTB_CELLULE_BO)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit de visualiser cette page");
		}

		HttpSession session = request.getSession();
		String buttonEffacer = request.getParameter(ACTION_PARAMETER_NAME);
		String parametreEtat = request.getParameter(ETAT_PARAMETER_NAME);
		String parametrePeriode = request.getParameter(PERIODE_PARAMETER_NAME);
		String parametreTypeMessage = request.getParameter(TYPE_MESSAGE_PARAMETER_NAME);


		boolean fromTraiteMessage = false;


		if (session.getAttribute(IDENTIFICATION_TRAITE_MESSAGE) != null) {
			fromTraiteMessage = (Boolean) session.getAttribute(IDENTIFICATION_TRAITE_MESSAGE);
		}

		// on remet a zero les critères de recherche afin

		boolean fromTableauDeBord = areUsed(parametreEtat, parametrePeriode, parametreTypeMessage);
		if (fromTableauDeBord || fromTraiteMessage) {
			removeModuleFromSession(request, IDENTIFICATION_CRITERIA_NAME);
		}

		IdentificationMessagesListView bean = (IdentificationMessagesListView) session.getAttribute(IDENTIFICATION_CRITERIA_NAME);

		if (bean == null || (buttonEffacer != null && buttonEffacer.equals(EFFACER_PARAMETER_VALUE))) {
			if (fromTableauDeBord) {
				bean = identificationMessagesListManager.getView(parametreTypeMessage, parametrePeriode, parametreEtat);
			}
			else {
				bean = identificationMessagesListManager.getView(null, null, null);
			}

			session.setAttribute(IDENTIFICATION_CRITERIA_NAME, bean);
		}


		return bean;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException,
	 *      java.util.Map)
	 */

	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model) throws Exception {

		HttpSession session = request.getSession();
		ModelAndView mav = super.showForm(request, response, errors, model);
		IdentificationMessagesListView bean = (IdentificationMessagesListView) session.getAttribute(IDENTIFICATION_CRITERIA_NAME);
		if (bean != null) {
			// Récupération de la pagination
			WebParamPagination pagination = new WebParamPagination(request, TABLE_IDENTIFICATION_ID, PAGE_SIZE);
			List<IdentificationMessagesResultView> listIdentifications = null;
			Integer nombreElements = 0;


			if (SecurityProvider.isGranted(Role.MW_IDENT_CTB_VISU) || SecurityProvider.isGranted(Role.MW_IDENT_CTB_ADMIN)) {

				listIdentifications = identificationMessagesListManager.find(bean, pagination, false, false, true);
				nombreElements = identificationMessagesListManager.count(bean, false, false, true);

			}
			else if (SecurityProvider.isGranted(Role.MW_IDENT_CTB_GEST_BO)) {
				listIdentifications = identificationMessagesListManager.find(bean, pagination, true, false, false);
				nombreElements = identificationMessagesListManager.count(bean, true, false, false);
			}
			else if (SecurityProvider.isGranted(Role.MW_IDENT_CTB_CELLULE_BO)) {
				listIdentifications = identificationMessagesListManager.findEncoursSeul(bean, pagination);
				nombreElements = identificationMessagesListManager.countEnCoursSeul(bean, null);
			}
			else if (SecurityProvider.isGranted(Role.NCS_IDENT_CTB_CELLULE_BO)) {
				listIdentifications = identificationMessagesListManager.findEncoursSeul(bean, pagination, TypeDemande.NCS);
				nombreElements = identificationMessagesListManager.countEnCoursSeul(bean, TypeDemande.NCS);
			}

			mav.addObject(IDENTIFICATION_LIST_ATTRIBUTE_NAME, listIdentifications);

			mav.addObject(IDENTIFICATION_LIST_ATTRIBUTE_SIZE, nombreElements);
		}
		else {
			mav.addObject(IDENTIFICATION_LIST_ATTRIBUTE_NAME, new ArrayList<IdentificationMessagesResultView>());
			mav.addObject(IDENTIFICATION_LIST_ATTRIBUTE_SIZE, 0);
		}

		mav.addObject(IDENTIFICATION_EN_COURS_MESSAGE, true);

		mav.addObject(IDENTIFICATION_TRAITE_MESSAGE, false);

		session.setAttribute(IDENTIFICATION_EN_COURS_MESSAGE, Boolean.TRUE);

		session.setAttribute(IDENTIFICATION_TRAITE_MESSAGE, Boolean.FALSE);

		return mav;
	}


	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {

		ModelAndView mav = super.onSubmit(request, response, command, errors);
		mav.setView(new RedirectView(getSuccessView()));

		IdentificationMessagesListView bean = (IdentificationMessagesListView) command;
		HttpSession session = request.getSession();

		if (request.getParameter(BOUTON_SUSPENDRE) != null) {
			identificationMessagesListManager.suspendreIdentificationMessages(bean);
			mav.setView(new RedirectView(getSuccessView()));
			return mav;
		}
		if (request.getParameter(BOUTON_SOUMETTRE) != null) {
			identificationMessagesListManager.ResoumettreIdentificationMessages(bean);
			mav.setView(new RedirectView(getSuccessView()));
			return mav;
		}

		session.setAttribute(IDENTIFICATION_CRITERIA_NAME, bean);
		if (request.getParameter(EFFACER_PARAMETER_VALUE) != null) {
			mav.setView(new RedirectView("listEnCours.do?action=effacer"));
		}
		else {
			mav.setView(new RedirectView(getSuccessView()));
		}
		return mav;
	}

	/**
	 * Removes the mapping for this module.
	 *
	 * @param	request	HttpRequest
	 * @param	module	Name of the specific module
	 */
	public static void removeModuleFromSession(HttpServletRequest request, String module) {
		HttpSession session = request.getSession(true);
		session.removeAttribute(module);
	}

	@Override
	public Map<Etat, String> initMapEtatMessage() {
		{
			if (SecurityProvider.isGranted(Role.MW_IDENT_CTB_VISU) || SecurityProvider.isGranted(Role.MW_IDENT_CTB_ADMIN)) {

				return identificationMapHelper.initMapEtatEnCoursSuspenduMessage();

			}
			else if (SecurityProvider.isGranted(Role.MW_IDENT_CTB_GEST_BO)) {
				return identificationMapHelper.initMapEtatEnCoursMessage();
			}
		}
		return null;
	}


	@Override
	protected Map<String, String> initMapEmetteurId() {
		return identificationMapHelper.initMapEmetteurId(false);

	}

	@Override
	protected Map<String, String> initMapTypeMessage() {
		if (SecurityProvider.isGranted(Role.MW_IDENT_CTB_CELLULE_BO) || SecurityProvider.isGranted(Role.MW_IDENT_CTB_ADMIN)
				|| SecurityProvider.isGranted(Role.MW_IDENT_CTB_VISU) || SecurityProvider.isGranted(Role.MW_IDENT_CTB_GEST_BO)) {
			//Les autres rôles ont le droit de voir tout les types de message
			return identificationMapHelper.initMapTypeMessage(false);
		}
		else if (SecurityProvider.isGranted(Role.NCS_IDENT_CTB_CELLULE_BO)) {

			//la cellule NCS ne voie que les messages de type NCS dont les certificats de salaire employeur
			return identificationMapHelper.initMapTypeMessage(false, TypeDemande.NCS);

		}
		else {
			return null;
		}
	}

	@Override
	protected Map<Integer, String> initMapPeriodeFiscale() {
		return identificationMapHelper.initMapPeriodeFiscale(false);

	}

	@Override
	protected Map<Demande.PrioriteEmetteur, String> initMapPrioriteEmetteur() {
		return identificationMapHelper.initMapPrioriteEmetteur(false);
	}
}
