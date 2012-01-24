package ch.vd.uniregctb.evenement;

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

import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPPCriteria;
import ch.vd.uniregctb.evenement.view.EvenementCivilView;
import ch.vd.uniregctb.evenement.view.EvenementCriteriaView;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.type.EtatEvenementCivil;

public class EvenementListController extends AbstractEvenementController {

	private static final String ACTION_PARAMETER_NAME = "action";
	private static final String EFFACER_PARAMETER_VALUE = "effacer";
	private static final String TABLE_NAME = "row";
	private static final int PAGE_SIZE = 25;
	private static final String DEFAULT_FIELD = "id";

	/**
	 * Un LOGGER.
	 */
	private final Logger LOGGER = Logger.getLogger(EvenementListController.class);

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		if (!SecurityProvider.isGranted(Role.EVEN)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec de gestion des événements civils");
		}
		final HttpSession session = request.getSession();
		EvenementCriteriaView bean = (EvenementCriteriaView) session.getAttribute(EVENEMENT_CRITERIA_NAME);

		if (bean == null || isAppuiSurEffacer(request)) {
	 		bean = (EvenementCriteriaView) super.formBackingObject(request);
			bean.setTypeRechercheDuNom(EvenementCivilRegPPCriteria.TypeRechercheDuNom.EST_EXACTEMENT);
			bean.setEtat(EtatEvenementCivil.A_VERIFIER);
			session.setAttribute(EVENEMENT_CRITERIA_NAME, bean);
			session.setAttribute(EVENEMENT_LIST_PAGE_INFO, null);
		}
		else {
			session.setAttribute(EVENEMENT_LIST_PAGE_INFO, ControllerUtils.getPaginatedTableParameters(request, TABLE_NAME));
		}

		return bean;
	}

	private static boolean isAppuiSurEffacer(HttpServletRequest request) {
		final String actionParameter = request.getParameter(ACTION_PARAMETER_NAME);
		return actionParameter != null && EFFACER_PARAMETER_VALUE.equals(actionParameter);
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse,
	 *      org.springframework.validation.BindException, java.util.Map)
	 */

	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model) throws Exception {

		final HttpSession session = request.getSession();
		final ModelAndView mav = super.showForm(request, response, errors, model);
		final EvenementCriteriaView bean = (EvenementCriteriaView) session.getAttribute(EVENEMENT_CRITERIA_NAME);
		if (bean != null && !isAppuiSurEffacer(request)) {
			// Récupération de la pagination
			// [UNIREG-1173] tri par défaut: id evt en décroissant (voir aussi evenement/list.jsp pour l'affichage dans la page)
			final WebParamPagination pagination = new WebParamPagination(request, TABLE_NAME, PAGE_SIZE, DEFAULT_FIELD, false);
			final List<EvenementCivilView> listEvenements = getEvenementManager().find(bean, pagination);

			mav.addObject(EVENEMENT_LIST_ATTRIBUTE_NAME, listEvenements);
			mav.addObject(EVENEMENT_LIST_ATTRIBUTE_SIZE, getEvenementManager().count(bean));
		}
		else {
			mav.addObject(EVENEMENT_LIST_ATTRIBUTE_NAME, new ArrayList<EvenementCivilRegPP>());
			mav.addObject(EVENEMENT_LIST_ATTRIBUTE_SIZE, 0);
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

		final ModelAndView mav = super.onSubmit(request, response, command, errors);
		mav.setView(new RedirectView(getSuccessView()));

		final EvenementCriteriaView bean = (EvenementCriteriaView) command;
		final HttpSession session = request.getSession();
		session.setAttribute(EVENEMENT_CRITERIA_NAME, bean);
		if (request.getParameter(EFFACER_PARAMETER_VALUE) != null) {
			mav.setView(new RedirectView("list.do?action=effacer"));
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
		final HttpSession session = request.getSession(true);
		session.removeAttribute(module);
	}

}
