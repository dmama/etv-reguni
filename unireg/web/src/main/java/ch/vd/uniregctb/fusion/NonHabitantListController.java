package ch.vd.uniregctb.fusion;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.tiers.AbstractTiersListController;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;

public class NonHabitantListController  extends  AbstractTiersListController {

	protected final Logger LOGGER = LoggerFactory.getLogger(NonHabitantListController.class);

	public static final String NON_HABITANT_CRITERIA_NAME = "nonHabitantCriteria";
	public static final String NON_HABITANT_LIST_ATTRIBUTE_NAME = "list";

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		HttpSession session = request.getSession();
		TiersCriteriaView  bean = (TiersCriteriaView) session.getAttribute(NON_HABITANT_CRITERIA_NAME);

		if (bean == null) {
			bean = (TiersCriteriaView) super.formBackingObject(request);
			bean.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.EST_EXACTEMENT);
			bean.setTypeTiersImperatif(TiersCriteria.TypeTiers.NON_HABITANT);
		}

		return bean;
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
		TiersCriteriaView bean = (TiersCriteriaView) session.getAttribute(NON_HABITANT_CRITERIA_NAME);
		ModelAndView mav = showFormForList(request, response, errors, model, NON_HABITANT_CRITERIA_NAME, NON_HABITANT_LIST_ATTRIBUTE_NAME, bean, true);
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

		HttpSession session = request.getSession();
		TiersCriteriaView bean = (TiersCriteriaView) command;

		session.setAttribute(NON_HABITANT_CRITERIA_NAME, bean);
		mav.setView(new RedirectView(getSuccessView()));
		return mav;
	}

}
