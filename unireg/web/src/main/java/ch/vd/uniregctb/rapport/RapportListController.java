package ch.vd.uniregctb.rapport;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.rapport.manager.RapportListManager;
import ch.vd.uniregctb.rapport.view.RapportListView;
import ch.vd.uniregctb.tiers.AbstractTiersListController;

public class RapportListController extends  AbstractTiersListController {

	protected final Logger LOGGER = Logger.getLogger(RapportListController.class);

	public static final String TIERS_LIE_CRITERIA_NAME = "tiersLieCriteria";
	public static final String TIERS_LIE_LIST_ATTRIBUTE_NAME = "list";

	private RapportListManager rapportListManager;

	private final String NUMERO_TIERS_PARAMETER_NAME = "numero";

	public static final String ACTION_PARAMETER_NAME = "action";
	public static final String ACTION_PARAMETER_EFFACER = "effacer";
	public static final String ACTION_PARAMETER_RECHERCHER = "rechercher";

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		HttpSession session = request.getSession();
		String action = request.getParameter(ACTION_PARAMETER_NAME);
		String numeroTiersParam = request.getParameter(NUMERO_TIERS_PARAMETER_NAME);
		Long numeroTiers = Long.parseLong(numeroTiersParam);

		RapportListView	rapportListView = (RapportListView) session.getAttribute(TIERS_LIE_CRITERIA_NAME);
		if(	(rapportListView == null) ||
			((action != null) && action.equals(EFFACER_PARAMETER_VALUE)) ) {
		 	//gestion des droits par rapportListeManager
			rapportListView = rapportListManager.get(numeroTiers);
		 }
		return rapportListView;
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
		RapportListView bean = (RapportListView) session.getAttribute(TIERS_LIE_CRITERIA_NAME);
		ModelAndView mav = showFormForList(request, response, errors, model, TIERS_LIE_CRITERIA_NAME, TIERS_LIE_LIST_ATTRIBUTE_NAME, bean, true);
		session.removeAttribute(TIERS_LIE_CRITERIA_NAME);
		return mav;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {

		ModelAndView mav = super.onSubmit(request, response, command, errors);

		RapportListView bean = (RapportListView) command;
		HttpSession session = request.getSession();
		session.setAttribute(TIERS_LIE_CRITERIA_NAME, bean);

		if (request.getParameter(BOUTON_RECHERCHER) != null) {
			mav.setView(new RedirectView("list.do?numero=" + bean.getTiers().getNumero()));
		} else if (request.getParameter(BOUTON_EFFACER) != null) {
			mav.setView(new RedirectView("list.do?numero=" + bean.getTiers().getNumero() + "&action=effacer"));
		}
		return mav;
	}

	public RapportListManager getRapportListManager() {
		return rapportListManager;
	}

	public void setRapportListManager(RapportListManager rapportListManager) {
		this.rapportListManager = rapportListManager;
	}


}
