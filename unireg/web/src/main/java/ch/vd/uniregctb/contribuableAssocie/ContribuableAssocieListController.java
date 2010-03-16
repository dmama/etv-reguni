package ch.vd.uniregctb.contribuableAssocie;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.contribuableAssocie.manager.ContribuableAssocieEditManager;
import ch.vd.uniregctb.contribuableAssocie.view.ContribuableAssocieListView;
import ch.vd.uniregctb.deces.DecesListController;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.TooManyResultsIndexerException;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.tiers.AbstractTiersListController;

public class ContribuableAssocieListController  extends  AbstractTiersListController {

	protected final Logger LOGGER = Logger.getLogger(DecesListController.class);

	private final String NUMERO_DEBITEUR_PARAMETER_NAME = "numeroDpi";

	public static final String ACTION_PARAMETER_NAME = "action";
	public static final String ACTION_PARAMETER_EFFACER = "effacer";
	public static final String ACTION_PARAMETER_RECHERCHER = "rechercher";

	public static final String CONTRIBUABLE_ASSOCIE_CRITERIA_NAME = "ContribuableAssocieCriteria";
	public static final String CONTRIBUABLE_ASSOCIE_LIST_ATTRIBUTE_NAME = "list";

	private ContribuableAssocieEditManager contribuableAssocieEditManager;

	public void setContribuableAssocieEditManager(ContribuableAssocieEditManager contribuableAssocieEditManager) {
		this.contribuableAssocieEditManager = contribuableAssocieEditManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		LOGGER.debug("Start of SeparationListController:formBackingObject");

		HttpSession session = request.getSession();
		String action = request.getParameter(ACTION_PARAMETER_NAME);
		String numeroDebiteurParam = request.getParameter(NUMERO_DEBITEUR_PARAMETER_NAME);

		ContribuableAssocieListView bean = (ContribuableAssocieListView) session.getAttribute(CONTRIBUABLE_ASSOCIE_CRITERIA_NAME);
		if(	(bean == null) ||
				((action != null) && action.equals(EFFACER_PARAMETER_VALUE)) ) {
			if (numeroDebiteurParam != null) {
				Long numeroDpi = Long.parseLong(numeroDebiteurParam);
				bean = contribuableAssocieEditManager.getContribuableList(numeroDpi);
			}
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
		ContribuableAssocieListView bean = (ContribuableAssocieListView) session.getAttribute(CONTRIBUABLE_ASSOCIE_CRITERIA_NAME);
		String buttonEffacer = request.getParameter(ACTION_PARAMETER_NAME);
		ModelAndView mav  =  super.showForm(request, response, errors, model);
		if (errors.getErrorCount() == 0) {
			if(buttonEffacer == null) {
				LOGGER.debug("Affichage du formulaire de recherche...");
				if (bean != null && !bean.isEmpty()) {
					LOGGER.debug("Critères de recherche=" + bean);
					if (StringUtils.isNotBlank(bean.getNumeroAVS())){
						bean.setNumeroAVS(FormatNumeroHelper.removeSpaceAndDash(bean.getNumeroAVS()));
					}
					try {
						List<TiersIndexedData> results = service.search(bean);
						mav.addObject(CONTRIBUABLE_ASSOCIE_LIST_ATTRIBUTE_NAME, results);
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
			mav.addObject(CONTRIBUABLE_ASSOCIE_LIST_ATTRIBUTE_NAME, null);
		}

		// TODO (fnr, fde) Cette ligne empeche la pagination sur les resultats de fonctionner.
		// Cependant j	e crois me rappeler que la supprimer engendre un autre effet de bord ...
		// A voir avec francois ...
		//
		// session.removeAttribute(CONTRIBUABLE_ASSOCIE_CRITERIA_NAME);

		return mav;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {

		String numeroDebiteurParam = request.getParameter(NUMERO_DEBITEUR_PARAMETER_NAME);

		LOGGER.debug("Traitement du formulaire...");
		ModelAndView mav = super.onSubmit(request, response, command, errors);

		ContribuableAssocieListView bean = (ContribuableAssocieListView) command;
		HttpSession session = request.getSession();
		session.setAttribute(CONTRIBUABLE_ASSOCIE_CRITERIA_NAME, bean);

		if (request.getParameter(BOUTON_EFFACER) != null) {
			mav.setView(new RedirectView("list.do?action=effacer&numeroDpi=" + numeroDebiteurParam ));
		} else {
			mav.setView(new RedirectView("list.do?numeroDpi=" + numeroDebiteurParam));
		}

		return mav;
	}

}
