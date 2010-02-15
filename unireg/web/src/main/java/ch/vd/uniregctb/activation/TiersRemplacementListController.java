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

import ch.vd.uniregctb.activation.manager.TiersRemplacementListManager;
import ch.vd.uniregctb.activation.view.TiersRemplacementListView;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.TooManyResultsIndexerException;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.tiers.AbstractTiersListController;

public class TiersRemplacementListController extends AbstractTiersListController{

	protected final Logger LOGGER = Logger.getLogger(TiersRemplacementListController.class);

	private TiersRemplacementListManager tiersRemplacementListManager;

	public void setTiersRemplacementListManager(TiersRemplacementListManager tiersRemplacementListManager) {
		this.tiersRemplacementListManager = tiersRemplacementListManager;
	}

	public static final String ACTION_PARAMETER_NAME = "action";
	public static final String TYPE_TIERS_PARAMETER_NAME = "type";
	public static final String ACTION_PARAMETER_EFFACER = "effacer";
	public static final String ACTION_PARAMETER_RECHERCHER = "rechercher";

	public static final String REMPLACEMENT_CRITERIA_NAME = "RemplacementCriteria";
	public static final String REMPLACEMENT_LIST_ATTRIBUTE_NAME = "list";

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		HttpSession session = request.getSession();
		String action = request.getParameter(ACTION_PARAMETER_NAME);
		String typeTiers = request.getParameter(TYPE_TIERS_PARAMETER_NAME);

		TiersRemplacementListView bean = (TiersRemplacementListView) session.getAttribute(REMPLACEMENT_CRITERIA_NAME);
		if(	(bean == null) ||
				((action != null) && action.equals(EFFACER_PARAMETER_VALUE)) ) {
			bean = tiersRemplacementListManager.get(typeTiers);
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
		TiersRemplacementListView bean = (TiersRemplacementListView) session.getAttribute(REMPLACEMENT_CRITERIA_NAME);
		String buttonEffacer = request.getParameter(ACTION_PARAMETER_NAME);
		ModelAndView mav  =  super.showForm(request, response, errors, model);
		if (errors.getErrorCount() == 0) {
			if(buttonEffacer == null) {
				LOGGER.debug("Affichage du formulaire de recherche...");
				if (bean != null && !bean.isEmpty()) {
					LOGGER.debug("Critères de recherche=" + bean);
					try {
						List<TiersIndexedData> results = service.search(bean);
						mav.addObject(REMPLACEMENT_LIST_ATTRIBUTE_NAME, results);
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
			mav.addObject(REMPLACEMENT_LIST_ATTRIBUTE_NAME, null);
		}
		session.removeAttribute(REMPLACEMENT_CRITERIA_NAME);

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

		TiersRemplacementListView bean = (TiersRemplacementListView) command;
		HttpSession session = request.getSession();
		session.setAttribute(REMPLACEMENT_CRITERIA_NAME, bean);

		mav.setView(new RedirectView("list.do"));

		return mav;
	}



}
