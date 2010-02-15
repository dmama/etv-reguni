package ch.vd.uniregctb.activation;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.activation.manager.TiersActivationListManager;
import ch.vd.uniregctb.activation.view.TiersActivationListView;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.TooManyResultsIndexerException;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.tiers.AbstractTiersListController;

public class TiersActivationListController extends AbstractTiersListController {

	protected final Logger LOGGER = Logger.getLogger(TiersActivationListController.class);

	private TiersActivationListManager tiersActivationListManager;

	public void setTiersActivationListManager(TiersActivationListManager tiersActivationListManager) {
		this.tiersActivationListManager = tiersActivationListManager;
	}

	public static final String ACTION_PARAMETER_NAME = "action";
	public static final String ACTIVATION_PARAMETER_NAME = "activation";
	public static final String ACTIVATION_ANNULATION_VALUE = "annulation";
	public static final String ACTIVATION_REACTIVATION_VALUE = "reactivation";
	public static final String ACTION_PARAMETER_EFFACER = "effacer";
	public static final String ACTION_PARAMETER_RECHERCHER = "rechercher";

	public static final String ACTIVATION_CRITERIA_NAME = "ActivationCriteria";
	public static final String ACTIVATION_LIST_ATTRIBUTE_NAME = "list";

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		HttpSession session = request.getSession();
		String action = request.getParameter(ACTION_PARAMETER_NAME);
		String activation = request.getParameter(ACTIVATION_PARAMETER_NAME);

		TiersActivationListView bean = (TiersActivationListView) session.getAttribute(ACTIVATION_CRITERIA_NAME);
		if(	(bean == null) ||
				((action != null) && action.equals(EFFACER_PARAMETER_VALUE)) ) {
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
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model)
			throws Exception {
		HttpSession session = request.getSession();
		TiersActivationListView bean = (TiersActivationListView) session.getAttribute(ACTIVATION_CRITERIA_NAME);
		String buttonEffacer = request.getParameter(ACTION_PARAMETER_NAME);
		ModelAndView mav  =  super.showForm(request, response, errors, model);
		if (errors.getErrorCount() == 0) {
			if(buttonEffacer == null) {
				LOGGER.debug("Affichage du formulaire de recherche...");
				if (bean != null && !bean.isEmpty()) {
					LOGGER.debug("Critères de recherche=" + bean);
					try {
						List<TiersIndexedData> results = service.search(bean);
						mav.addObject(ACTIVATION_LIST_ATTRIBUTE_NAME, results);
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
			mav.addObject(ACTIVATION_LIST_ATTRIBUTE_NAME, null);
		}
		session.removeAttribute(ACTIVATION_CRITERIA_NAME);

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

		String activation = request.getParameter(ACTIVATION_PARAMETER_NAME);

		TiersActivationListView bean = (TiersActivationListView) command;
		HttpSession session = request.getSession();
		session.setAttribute(ACTIVATION_CRITERIA_NAME, bean);

		if (request.getParameter(BOUTON_EFFACER) != null) {
			mav.setView(new RedirectView("list.do?activation=" + activation + "&action=effacer"));
		} else {
			mav.setView(new RedirectView("list.do?activation=" + activation));
		}

		return mav;
	}


}
