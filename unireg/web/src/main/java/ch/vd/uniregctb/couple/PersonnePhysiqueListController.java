package ch.vd.uniregctb.couple;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.couple.manager.CoupleListManager;
import ch.vd.uniregctb.couple.view.CoupleListView;
import ch.vd.uniregctb.tiers.AbstractTiersListController;
import ch.vd.uniregctb.tracing.TracePoint;
import ch.vd.uniregctb.tracing.TracingManager;

public class PersonnePhysiqueListController extends  AbstractTiersListController {

	protected final Logger LOGGER = Logger.getLogger(PersonnePhysiqueListController.class);

	private CoupleListManager coupleListManager;

	private static final String PREMIER_TIERS = "premier";
	private static final String SECOND_TIER = "second";
	private static final String TROISIEME_TIER = "troisième";

	public static final String PP_CRITERIA_NAME = "ppCriteria";
	public static final String PP_LIST_ATTRIBUTE_NAME = "list";

	public static final String BUTTON_POURSUIVRE = "poursuivre";

	private static final String NUMERO_PREMIER_PP_PARAMETER_NAME = "numeroPP1";
	private static final String CLEAR_SESSION_PARAMETER_NAME = "clearSession";

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		TracePoint tp = TracingManager.begin();

		String numeroPremierePersonneParam = request.getParameter(NUMERO_PREMIER_PP_PARAMETER_NAME);
		Long numeroPremierePersonne = null;
		if (numeroPremierePersonneParam != null) {
			numeroPremierePersonne= Long.parseLong(numeroPremierePersonneParam);
		}

		// [SIFISC-2459] Hack pour s'assurer que rien n'est gardé en session lorsqu'on arrive depuis le raccourci des actions du bandeau de tiers
		final String clearSessionParam = request.getParameter(CLEAR_SESSION_PARAMETER_NAME);
		if (clearSessionParam != null && clearSessionParam.equalsIgnoreCase("true")) {
			removeModuleFromSession(request, PP_CRITERIA_NAME);
			CoupleRecapController.effaceCriteresRecherche(request);
		}

		HttpSession session = request.getSession();
		CoupleListView  coupleListView = (CoupleListView) session.getAttribute(PP_CRITERIA_NAME);
		String action = request.getParameter(ACTION_PARAMETER_NAME);

		boolean actionEffacer = EFFACER_PARAMETER_VALUE.equals(action);

		if (coupleListView == null || actionEffacer) {
			coupleListView = coupleListManager.get();
		}
		if (PREMIER_TIERS.equals(coupleListView.getPage())) {
			if (numeroPremierePersonne != null) {
				coupleListView = coupleListManager.get(numeroPremierePersonne);
				removeModuleFromSession(request, PP_CRITERIA_NAME);
			}
			else {
				CoupleRecapController.effaceCriteresRecherche(request);
			}
		}
		else if (SECOND_TIER.equals(coupleListView.getPage())) {
			if (numeroPremierePersonne == null) {
				coupleListView = coupleListManager.get();
				removeModuleFromSession(request, PP_CRITERIA_NAME);
			}
		}
		else if (TROISIEME_TIER.equals(coupleListView.getPage())) {

		}

		TracingManager.end(tp);
		return coupleListView;
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
		CoupleListView bean = (CoupleListView) session.getAttribute(PP_CRITERIA_NAME);
		return showFormForList(request, response, errors, model, PP_CRITERIA_NAME, PP_LIST_ATTRIBUTE_NAME, bean, true);
	}


	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {

		ModelAndView mav = super.onSubmit(request, response, command, errors);

		CoupleListView coupleListView = (CoupleListView) command;

		HttpSession session = request.getSession();
		if (request.getParameter(BOUTON_RECHERCHER) != null) {
			if (coupleListView.getPage().equals(PREMIER_TIERS)) {
				mav.setView(new RedirectView(getSuccessView()));
				session.setAttribute(PP_CRITERIA_NAME, coupleListView);
			} else if (coupleListView.getPage().equals(SECOND_TIER)) {
				mav.setView(new RedirectView("list-pp.do?numeroPP1=" + coupleListView.getNumeroPremierePersonne() ));
				session.setAttribute(PP_CRITERIA_NAME, coupleListView);
			}
		} else if (request.getParameter(BOUTON_EFFACER) != null) {
			if (coupleListView.getPage().equals(PREMIER_TIERS)) {
				mav.setView(new RedirectView("list-pp.do?action=effacer"));
			} else if (coupleListView.getPage().equals(SECOND_TIER)) {
				mav.setView(new RedirectView("list-pp.do?numeroPP1=" + coupleListView.getNumeroPremierePersonne() + "&action=effacer" ));
			}
		}
		else if (request.getParameter(BUTTON_POURSUIVRE) != null) {
			// conjoint inconnu sélectionné
			if (coupleListView.isConjointInconnu()) {
				mav.setView(new RedirectView("recap.do?numeroPP1=" + coupleListView.getNumeroPremierePersonne()));
			}
		}
		return mav;
	}

	public CoupleListManager getCoupleListManager() {
		return coupleListManager;
	}

	public void setCoupleListManager(CoupleListManager coupleListManager) {
		this.coupleListManager = coupleListManager;
	}


}
