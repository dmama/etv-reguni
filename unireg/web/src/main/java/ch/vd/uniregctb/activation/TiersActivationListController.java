package ch.vd.uniregctb.activation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.activation.manager.TiersActivationListManager;
import ch.vd.uniregctb.activation.view.TiersActivationListView;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.TooManyResultsIndexerException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.tiers.AbstractTiersListController;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersIndexedDataView;

public class TiersActivationListController extends AbstractTiersListController {

	protected final Logger LOGGER = LoggerFactory.getLogger(TiersActivationListController.class);

	private TiersActivationListManager tiersActivationListManager;

	public void setTiersActivationListManager(TiersActivationListManager tiersActivationListManager) {
		this.tiersActivationListManager = tiersActivationListManager;
	}

	public static final String ACTION_PARAMETER_NAME = "action";
	public static final String ACTIVATION_PARAMETER_NAME = "activation";
	public static final String ACTIVATION_ANNULATION_VALUE = "annulation";
	public static final String ACTIVATION_REACTIVATION_VALUE = "reactivation";

	public static final String ACTIVATION_CRITERIA_NAME = "ActivationCriteria";
	public static final String ACTIVATION_LIST_ATTRIBUTE_NAME = "list";

	private static boolean isAnnulation(HttpServletRequest request) {
		final String activation = request.getParameter(ACTIVATION_PARAMETER_NAME);
		return ACTIVATION_ANNULATION_VALUE.equals(activation);
	}

	private static boolean isReactivation(HttpServletRequest request) {
		final String activation = request.getParameter(ACTIVATION_PARAMETER_NAME);
		return ACTIVATION_REACTIVATION_VALUE.equals(activation);
	}

	private static String getCriteriaSessionAttributeName(HttpServletRequest request) {
		final String activation = request.getParameter(ACTIVATION_PARAMETER_NAME);
		return getCriteriaSessionAttributeName(activation);
	}

	public static String getCriteriaSessionAttributeName(String activation) {
		return String.format("%s_%s", ACTIVATION_CRITERIA_NAME, activation);
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		final HttpSession session = request.getSession();
		final String activation = request.getParameter(ACTIVATION_PARAMETER_NAME);

		TiersActivationListView bean = (TiersActivationListView) session.getAttribute(getCriteriaSessionAttributeName(activation));
		if (bean == null) {
			bean = tiersActivationListManager.get(activation);
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
		final TiersActivationListView bean = (TiersActivationListView) session.getAttribute(getCriteriaSessionAttributeName(request));
		final ModelAndView mav = super.showForm(request, response, errors, model);
		if (errors.getErrorCount() == 0) {
			if (bean != null && !bean.isEmpty()) {
				try {
					final boolean isAnnulation = isAnnulation(request);
					final boolean isReactivation = isReactivation(request);

					// [SIFISC-18536] validation du type de tiers à rechercher en fonction des droits
					final Set<TiersCriteria.TypeTiers> typesTiersAutorises = EnumSet.noneOf(TiersCriteria.TypeTiers.class);
					if (SecurityHelper.isAnyGranted(securityProvider, Role.MODIF_PM)) {
						typesTiersAutorises.add(TiersCriteria.TypeTiers.ENTREPRISE);
						typesTiersAutorises.add(TiersCriteria.TypeTiers.ETABLISSEMENT_SECONDAIRE);
					}
					if (SecurityHelper.isAnyGranted(securityProvider, Role.MODIF_VD_ORD, Role.MODIF_VD_SOURC, Role.MODIF_HC_HS, Role.MODIF_HAB_DEBPUR, Role.MODIF_NONHAB_DEBPUR)) {
						typesTiersAutorises.add(TiersCriteria.TypeTiers.DEBITEUR_PRESTATION_IMPOSABLE);
						typesTiersAutorises.add(TiersCriteria.TypeTiers.AUTRE_COMMUNAUTE);
						typesTiersAutorises.add(TiersCriteria.TypeTiers.COLLECTIVITE_ADMINISTRATIVE);
						typesTiersAutorises.add(TiersCriteria.TypeTiers.CONTRIBUABLE_PP);
					}
					bean.setTypesTiersImperatifs(typesTiersAutorises);

					final List<TiersIndexedDataView> results = searchTiers(bean);
					if (isAnnulation) {
						filterOutAnnules(results);
					}
					else if (isReactivation) {
						filterOutNonAnnules(results);
					}
					mav.addObject(ACTIVATION_LIST_ATTRIBUTE_NAME, results);
				}
				catch (TooManyResultsIndexerException ee) {
					LOGGER.error("Exception dans l'indexer: " + ee.getMessage(), ee);
					if (ee.getNbResults() > 0) {
						errors.reject("error.preciser.recherche.trouves", new Object[] {String.valueOf(ee.getNbResults())}, null);
					}
					else {
						errors.reject("error.preciser.recherche");
					}
				}
				catch (IndexerException e) {
					LOGGER.error("Exception dans l'indexer: " + e.getMessage(), e);
					errors.reject("error.recherche");
				}
			}
		}
		else {
			mav.addObject(ACTIVATION_LIST_ATTRIBUTE_NAME, null);
		}

		return mav;
	}

	private static void filterOutAnnules(List<TiersIndexedDataView> views) {
		CollectionUtils.filter(views, view -> !view.isAnnule());
	}

	private static void filterOutNonAnnules(final List<TiersIndexedDataView> views) {
		CollectionUtils.filter(views, TiersIndexedDataView::isAnnule);
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {

		ModelAndView mav = super.onSubmit(request, response, command, errors);

		String activation = request.getParameter(ACTIVATION_PARAMETER_NAME);

		TiersActivationListView bean = (TiersActivationListView) command;
		HttpSession session = request.getSession();
		session.setAttribute(getCriteriaSessionAttributeName(activation), bean);

		mav.setView(new RedirectView("list.do?activation=" + activation));
		return mav;
	}


}
