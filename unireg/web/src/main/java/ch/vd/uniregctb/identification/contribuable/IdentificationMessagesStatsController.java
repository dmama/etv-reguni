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

import ch.vd.uniregctb.identification.contribuable.manager.IdentificationMessagesStatsManager;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesStatsResultView;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesStatsView;

public class IdentificationMessagesStatsController extends AbstractIdentificationController {

	protected static final Logger LOGGER = Logger.getLogger(IdentificationMessagesStatsController.class);



	private static final String ACTION_PARAMETER_NAME = "action";
	private static final String EFFACER_PARAMETER_VALUE = "effacer";
	public static final String STATS_CRITERIA_NAME = "statsCriteria";
	public static final String RESULT_SIZE_NAME = "resultSize";
	public static final String IDENTIFICATION_STATS_ATTRIBUTE_NAME = "statistiques";


	private IdentificationMessagesStatsManager identificationMessagesStatsManager;



	public void setIdentificationMessagesStatsManager(IdentificationMessagesStatsManager identificationMessagesStatsManager) {
		this.identificationMessagesStatsManager = identificationMessagesStatsManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		LOGGER.debug("Start of IdentificationMessagesStatController:formBackingObject");
		HttpSession session = request.getSession();
		String buttonEffacer = request.getParameter(ACTION_PARAMETER_NAME);

		IdentificationMessagesStatsView bean = (IdentificationMessagesStatsView) session.getAttribute(STATS_CRITERIA_NAME);

		if (bean == null || (buttonEffacer != null && buttonEffacer.equals(EFFACER_PARAMETER_VALUE))) {
	 		bean = identificationMessagesStatsManager.getView();
			session.setAttribute(STATS_CRITERIA_NAME, bean);
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
		IdentificationMessagesStatsView bean = (IdentificationMessagesStatsView) session.getAttribute(STATS_CRITERIA_NAME);
		if (bean != null) {

			List<IdentificationMessagesStatsResultView> listResultat = identificationMessagesStatsManager.calculerStats(bean);

			mav.addObject(IDENTIFICATION_STATS_ATTRIBUTE_NAME, listResultat);

		}
		else {
			mav.addObject(IDENTIFICATION_STATS_ATTRIBUTE_NAME, new ArrayList<IdentificationMessagesStatsResultView>());

		}


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

		IdentificationMessagesStatsView bean = (IdentificationMessagesStatsView) command;
		HttpSession session = request.getSession();


		session.setAttribute(STATS_CRITERIA_NAME, bean);
		if (request.getParameter(EFFACER_PARAMETER_VALUE) != null) {
			mav.setView(new RedirectView("stats.do?action=effacer"));
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
}
