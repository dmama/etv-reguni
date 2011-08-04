package ch.vd.uniregctb.deces;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.deces.manager.DecesRecapManager;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.TooManyResultsIndexerException;
import ch.vd.uniregctb.tiers.AbstractTiersListController;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersIndexedDataView;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;

public class DecesListController extends  AbstractTiersListController {

	protected final Logger LOGGER = Logger.getLogger(DecesListController.class);

	public static final String ACTION_PARAMETER_NAME = "action";
	public static final String ACTION_PARAMETER_EFFACER = "effacer";
	public static final String ACTION_PARAMETER_RECHERCHER = "rechercher";

	public static final String DECES_CRITERIA_NAME = "DecesCriteria";
	public static final String DECES_LIST_ATTRIBUTE_NAME = "list";

	private DecesRecapManager decesRecapManager;

	public void setDecesRecapManager(DecesRecapManager decesRecapManager) {
		this.decesRecapManager = decesRecapManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		final HttpSession session = request.getSession();
		final String action = request.getParameter(ACTION_PARAMETER_NAME);

		TiersCriteriaView bean = (TiersCriteriaView) session.getAttribute(DECES_CRITERIA_NAME);
		if (bean == null || (action != null && action.equals(EFFACER_PARAMETER_VALUE))) {
			bean = new TiersCriteriaView();
			bean.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.EST_EXACTEMENT);
			bean.setTypeTiers(TiersCriteria.TypeTiers.PERSONNE_PHYSIQUE);
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
		final TiersCriteriaView bean = (TiersCriteriaView) session.getAttribute(DECES_CRITERIA_NAME);
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
						final List<TiersIndexedDataView> filtredResults = new ArrayList<TiersIndexedDataView>();
						for (TiersIndexedDataView tiersIndexedData : results) {
							if (!decesRecapManager.isDecede(tiersIndexedData.getNumero())) {
								filtredResults.add(tiersIndexedData);
							}
						}
						mav.addObject(DECES_LIST_ATTRIBUTE_NAME, filtredResults);
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
		else {
			mav.addObject(DECES_LIST_ATTRIBUTE_NAME, null);
		}
					
		return mav;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {

		final ModelAndView mav = super.onSubmit(request, response, command, errors);

		final TiersCriteriaView bean = (TiersCriteriaView) command;
		final HttpSession session = request.getSession();
		session.setAttribute(DECES_CRITERIA_NAME, bean);

		if (request.getParameter(BOUTON_EFFACER) != null) {
			mav.setView(new RedirectView("list.do?action=effacer"));
		} else {
			mav.setView(new RedirectView("list.do"));
		}

		return mav;
	}

}
