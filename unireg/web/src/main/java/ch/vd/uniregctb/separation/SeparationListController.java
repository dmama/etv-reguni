package ch.vd.uniregctb.separation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.TooManyResultsIndexerException;
import ch.vd.uniregctb.separation.manager.SeparationRecapManager;
import ch.vd.uniregctb.tiers.AbstractTiersListController;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersIndexedDataView;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;

public class SeparationListController extends  AbstractTiersListController {

	protected final Logger LOGGER = Logger.getLogger(SeparationListController.class);

	public static final String ACTION_PARAMETER_NAME = "action";

	public static final String SEPARATION_CRITERIA_NAME = "SeparationCriteria";
	public static final String SEPARATION_LIST_ATTRIBUTE_NAME = "list";

	private SeparationRecapManager separationRecapManager;

	public void setSeparationRecapManager(SeparationRecapManager separationRecapManager) {
		this.separationRecapManager = separationRecapManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		final HttpSession session = request.getSession();
		final String action = request.getParameter(ACTION_PARAMETER_NAME);

		TiersCriteriaView bean = (TiersCriteriaView) session.getAttribute(SEPARATION_CRITERIA_NAME);
		if (bean == null) {
			bean = new TiersCriteriaView();
			bean.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.EST_EXACTEMENT);
			bean.setTypeTiersImperatif(TiersCriteria.TypeTiers.MENAGE_COMMUN);
	 	}
		return bean;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException, java.util.Map)
	 */

	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model) throws Exception {
		final HttpSession session = request.getSession();
		final TiersCriteriaView bean = (TiersCriteriaView) session.getAttribute(SEPARATION_CRITERIA_NAME);
		final String buttonEffacer = request.getParameter(ACTION_PARAMETER_NAME);
		final ModelAndView mav  =  super.showForm(request, response, errors, model);
		if (errors.getErrorCount() == 0) {
			if (buttonEffacer == null) {
				LOGGER.debug("Affichage du formulaire de recherche...");
				if (bean != null && !bean.isEmpty()) {
					LOGGER.debug("Critères de recherche=" + bean);
					if (StringUtils.isNotBlank(bean.getNumeroAVS())){
						bean.setNumeroAVS(FormatNumeroHelper.removeSpaceAndDash(bean.getNumeroAVS()));
					}
					try {
						final List<TiersIndexedDataView> results = searchTiers(bean);
						final List<TiersIndexedDataView> filtredResults = new ArrayList<>();
						for (TiersIndexedDataView tiersIndexedData : results) {
							if (separationRecapManager.isAvecForFiscalPrincipalActif(tiersIndexedData.getNumero())) {
								filtredResults.add(tiersIndexedData);
							}
						}
						mav.addObject(SEPARATION_LIST_ATTRIBUTE_NAME, filtredResults);
					}
					catch (TooManyResultsIndexerException ee) {
						LOGGER.error("Exception dans l'indexer: " + ee.getMessage(), ee);
						errors.reject("error.preciser.recherche");
					}
					catch (IndexerException e) {
						LOGGER.error("Exception dans l'indexer: " + e.getMessage(), e);
						errors.reject("error.recherche");
					}
				}
			}
		}
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

		TiersCriteriaView bean = (TiersCriteriaView) command;
		HttpSession session = request.getSession();
		session.setAttribute(SEPARATION_CRITERIA_NAME, bean);

		mav.setView(new RedirectView("list.do"));
		return mav;
	}
}
