package ch.vd.uniregctb.identification.contribuable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;
import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;
import ch.vd.uniregctb.identification.contribuable.manager.IdentificationMessagesListManager;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesListView;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesResultView;

public class IdentificationMessagesListController extends AbstractIdentificationController {

	protected static final Logger LOGGER = Logger.getLogger(IdentificationMessagesListController.class);

	public final static String BOUTON_SUSPENDRE = "suspendre";
	public final static String BOUTON_SOUMETTRE = "soumettre";

	private static final String ACTION_PARAMETER_NAME = "action";
	private static final String EFFACER_PARAMETER_VALUE = "effacer";
	public static final String IDENTIFICATION_CRITERIA_NAME = "identificationCriteria";
	public static final String RESULT_SIZE_NAME = "resultSize";
	public static final Integer PAGE_SIZE = new Integer(25);
	public static final String IDENTIFICATION_LIST_ATTRIBUTE_NAME = "identifications";
	public static final String IDENTIFICATION_LIST_ATTRIBUTE_SIZE = "identificationsSize";
	private static final String TABLE_IDENTIFICATION_ID = "message";
	private static final String ETAT_PARAMETER_NAME = "etat";
	private static final String TYPE_MESSAGE_PARAMETER_NAME = "typeMessage";
	private static final String PERIODE_PARAMETER_NAME = "periode";
	private static final String IDENTIFICATION_GESTION_MESSAGE = "gestionMessage";



	private IdentificationMessagesListManager identificationMessagesListManager;

	public void setIdentificationMessagesListManager(IdentificationMessagesListManager identificationMessagesListManager) {
		this.identificationMessagesListManager = identificationMessagesListManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		HttpSession session = request.getSession();
		String buttonEffacer = request.getParameter(ACTION_PARAMETER_NAME);
		String parametreEtat = request.getParameter(ETAT_PARAMETER_NAME);
		String parametrePeriode = request.getParameter(PERIODE_PARAMETER_NAME);
		String parametreTypeMessage = request.getParameter(TYPE_MESSAGE_PARAMETER_NAME);
		// on remet a zero les critères de recherche afin d'utiliser ceux envoyé par le tableau de bord
		if (areUsed(parametreEtat, parametrePeriode, parametreTypeMessage)) {
			removeModuleFromSession(request, IDENTIFICATION_CRITERIA_NAME);
		}

		IdentificationMessagesListView bean = (IdentificationMessagesListView) session.getAttribute(IDENTIFICATION_CRITERIA_NAME);

		boolean fromGestionMessage = false;
		Boolean fromTraiteMessage = false;
		if (session.getAttribute(IDENTIFICATION_GESTION_MESSAGE)!=null) {
			fromGestionMessage = (Boolean)session.getAttribute(IDENTIFICATION_GESTION_MESSAGE);
		}
		if (session.getAttribute(IDENTIFICATION_TRAITE_MESSAGE)!=null) {
			 fromTraiteMessage = (Boolean)session.getAttribute(IDENTIFICATION_TRAITE_MESSAGE);
		}


		if (bean == null || (buttonEffacer != null && buttonEffacer.equals(EFFACER_PARAMETER_VALUE)) || !fromGestionMessage || fromTraiteMessage) {
	 		bean = identificationMessagesListManager.getView(parametreTypeMessage, parametrePeriode, parametreEtat);
			session.setAttribute(IDENTIFICATION_CRITERIA_NAME, bean);
		}

		return bean;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse,
	 *      org.springframework.validation.BindException, java.util.Map)
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

			List<IdentificationMessagesResultView> listIdentifications = identificationMessagesListManager.find(bean, pagination, false, false,true, TypeDemande.MELDEWESEN);

			mav.addObject(IDENTIFICATION_LIST_ATTRIBUTE_NAME, listIdentifications);
			mav.addObject(IDENTIFICATION_LIST_ATTRIBUTE_SIZE, Integer.valueOf(identificationMessagesListManager.count(bean, false, false,true, TypeDemande.MELDEWESEN)));
		}
		else {
			mav.addObject(IDENTIFICATION_LIST_ATTRIBUTE_NAME, new ArrayList<IdentificationMessagesResultView>());
			mav.addObject(IDENTIFICATION_LIST_ATTRIBUTE_SIZE, Integer.valueOf(0));
		}
		mav.addObject(IDENTIFICATION_EN_COURS_MESSAGE, false);
		mav.addObject(IDENTIFICATION_GESTION_MESSAGE, true);
		mav.addObject(IDENTIFICATION_TRAITE_MESSAGE, false);

		session.setAttribute(IDENTIFICATION_EN_COURS_MESSAGE,Boolean.FALSE);
		session.setAttribute(IDENTIFICATION_GESTION_MESSAGE,Boolean.TRUE);
		session.setAttribute(IDENTIFICATION_TRAITE_MESSAGE,Boolean.FALSE);
		return mav;
	}




	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
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
			mav.setView(new RedirectView("list.do?action=effacer"));
		} else {
			mav.setView(new RedirectView(getSuccessView()));
		}
		return mav;
	}


	/**
	 * Initialise la map des états du message en fonction du type de controleur
	 * @return une map
	 */
	@Override
	protected  Map<Etat, String> initMapEtatMessage() {

			return identificationMapHelper.initMapEtatEnCoursSuspenduMessage();

	}

	/**
	 * Removes the mapping for this module.
	 * @param	request	HttpRequest
	 * @param	module	Name of the specific module
	 */
	public static void removeModuleFromSession(HttpServletRequest request, String module) {
		HttpSession session = request.getSession(true);
		session.removeAttribute(module);
	}




}
