package ch.vd.uniregctb.fusion;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.fusion.manager.FusionListManager;
import ch.vd.uniregctb.fusion.view.FusionListView;
import ch.vd.uniregctb.tiers.AbstractTiersListController;
import ch.vd.uniregctb.tracing.TracePoint;
import ch.vd.uniregctb.tracing.TracingManager;

public class HabitantListController  extends  AbstractTiersListController {

	protected final Logger LOGGER = Logger.getLogger(HabitantListController.class);

	public static final String HABITANT_CRITERIA_NAME = "habitantCriteria";
	public static final String HABITANT_LIST_ATTRIBUTE_NAME = "list";

	private static final String NUMERO_NON_HABITANT_PARAMETER_NAME = "numeroNonHab";

	private FusionListManager fusionListManager;

	public FusionListManager getFusionListManager() {
		return fusionListManager;
	}

	public void setFusionListManager(FusionListManager fusionListManager) {
		this.fusionListManager = fusionListManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		String action = request.getParameter(ACTION_PARAMETER_NAME);
		String numeroNonHabParam = request.getParameter(NUMERO_NON_HABITANT_PARAMETER_NAME);
		Long numeroNonHab= null;
		if (numeroNonHabParam != null) {
			numeroNonHab= Long.parseLong(numeroNonHabParam);
		}

		HttpSession session = request.getSession();
		FusionListView  fusionListView = (FusionListView) session.getAttribute(HABITANT_CRITERIA_NAME);

		if(	(fusionListView == null) ||
			((action != null) && action.equals(EFFACER_PARAMETER_VALUE)) ) {
			fusionListView = fusionListManager.get(numeroNonHab);
		}

		return fusionListView;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException, java.util.Map)
	 */

	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model)
			throws Exception {

		HttpSession session = request.getSession();
		FusionListView bean = (FusionListView) session.getAttribute(HABITANT_CRITERIA_NAME);
		ModelAndView mav = showFormForList(request, response, errors, model, HABITANT_CRITERIA_NAME, HABITANT_LIST_ATTRIBUTE_NAME, bean, true);
		return mav;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {

		TracePoint tp = TracingManager.begin();
		ModelAndView mav = super.onSubmit(request, response, command, errors);

		FusionListView bean = (FusionListView) command;
		HttpSession session = request.getSession();

		if (request.getParameter(BOUTON_RECHERCHER) != null) {
			session.setAttribute(HABITANT_CRITERIA_NAME, bean);
			mav.setView(new RedirectView("list-habitant.do?numeroNonHab=" + bean.getNumeroNonHabitant() ));
		} else if (request.getParameter(BOUTON_EFFACER) != null) {
			mav.setView(new RedirectView("list-habitant.do?numeroNonHab=" + bean.getNumeroNonHabitant() +"&action=effacer"));
		}
		TracingManager.end(tp);

		TracingManager.outputMeasures(LOGGER);

		return mav;
	}

}
