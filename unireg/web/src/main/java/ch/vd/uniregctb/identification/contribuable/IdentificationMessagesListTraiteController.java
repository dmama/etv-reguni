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

public class IdentificationMessagesListTraiteController extends AbstractIdentificationController {

	protected static final Logger LOGGER = Logger.getLogger(IdentificationMessagesListTraiteController.class);


	private static final String ACTION_PARAMETER_NAME = "action";
	private static final String EFFACER_PARAMETER_VALUE = "effacer";
	public static final String IDENTIFICATION_CRITERIA_NAME = "identificationCriteria";
	public static final String RESULT_SIZE_NAME = "resultSize";
	public static final Integer PAGE_SIZE = new Integer(25);
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


		LOGGER.debug("Start of IdentificationMessagesListTraiteController:formBackingObject");

		HttpSession session = request.getSession();
		String buttonEffacer = request.getParameter(ACTION_PARAMETER_NAME);
		String parametreEtat = request.getParameter(ETAT_PARAMETER_NAME);
		String parametrePeriode = request.getParameter(PERIODE_PARAMETER_NAME);
		String parametreTypeMessage = request.getParameter(TYPE_MESSAGE_PARAMETER_NAME);

		boolean fromMessageEnCours = false;



		if (session.getAttribute(IDENTIFICATION_EN_COURS_MESSAGE)!=null) {
			fromMessageEnCours = (Boolean)session.getAttribute(IDENTIFICATION_EN_COURS_MESSAGE);
		}

		// on remet a zero les critères de recherche afin

		boolean fromTableauDeBord = areUsed(parametreEtat, parametrePeriode, parametreTypeMessage);

		if (fromTableauDeBord || fromMessageEnCours) {
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

			List<IdentificationMessagesResultView> listIdentifications = identificationMessagesListManager.find(bean, pagination, false, true,false, TypeDemande.MELDEWESEN);

			mav.addObject(IDENTIFICATION_LIST_ATTRIBUTE_NAME, listIdentifications);
			mav.addObject(IDENTIFICATION_LIST_ATTRIBUTE_SIZE, Integer.valueOf(identificationMessagesListManager.count(bean,false, true,false, TypeDemande.MELDEWESEN)));
		}
		else {
			mav.addObject(IDENTIFICATION_LIST_ATTRIBUTE_NAME, new ArrayList<IdentificationMessagesResultView>());
			mav.addObject(IDENTIFICATION_LIST_ATTRIBUTE_SIZE, Integer.valueOf(0));
		}
		mav.addObject(IDENTIFICATION_EN_COURS_MESSAGE, false);

		mav.addObject(IDENTIFICATION_TRAITE_MESSAGE, true);

		session.setAttribute(IDENTIFICATION_EN_COURS_MESSAGE,Boolean.FALSE);
		session.setAttribute(IDENTIFICATION_TRAITE_MESSAGE,Boolean.TRUE);

		return mav;
	}




	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {

		LOGGER.debug("Traitement du formulaire...");
		ModelAndView mav = super.onSubmit(request, response, command, errors);
		mav.setView(new RedirectView(getSuccessView()));

		IdentificationMessagesListView bean = (IdentificationMessagesListView) command;
		HttpSession session = request.getSession();

		session.setAttribute(IDENTIFICATION_CRITERIA_NAME, bean);
		if (request.getParameter(EFFACER_PARAMETER_VALUE) != null) {
			mav.setView(new RedirectView("listTraite.do?action=effacer"));
		} else {
			mav.setView(new RedirectView(getSuccessView()));
		}
		return mav;
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

	@Override
	public Map<Etat, String> initMapEtatMessage() {
		{
			return identificationMapHelper.initMapEtatArchivewMessage();
		}
	}

}
