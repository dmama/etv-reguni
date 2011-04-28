package ch.vd.uniregctb.acces.parUtilisateur;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.acces.parUtilisateur.view.UtilisateurListPersonneView;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.general.manager.UtilisateurManager;
import ch.vd.uniregctb.general.view.UtilisateurView;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.TooManyResultsIndexerException;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.AbstractTiersListController;
import ch.vd.uniregctb.tiers.TiersIndexedDataView;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;

public class UtilisateurListPersonneController extends AbstractTiersListController {

	protected final Logger LOGGER = Logger.getLogger(UtilisateurListPersonneController.class);

	private UtilisateurManager utilisateurManager;

	public static final String UTILISATEUR_PARAMETER_NAME = "noIndividuOperateur";

	public static final String ACTION_PARAMETER_NAME = "action";
	public static final String ACTION_PARAMETER_EFFACER = "effacer";
	public static final String ACTION_PARAMETER_RECHERCHER = "rechercher";

	public static final String PERSONNE_CRITERIA_NAME = "PersonneCriteria";
	public static final String PERSONNE_LIST_ATTRIBUTE_NAME = "list";

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		if (!SecurityProvider.isGranted(Role.SEC_DOS_ECR)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec pour modifier à la sécurité des droits");
		}
		
		HttpSession session = request.getSession();
		String action = request.getParameter(ACTION_PARAMETER_NAME);
		String noIndividuOperateurParam = request.getParameter(UTILISATEUR_PARAMETER_NAME);
		long noIndividuOperateur = 0;
		if (noIndividuOperateurParam != null) {
			noIndividuOperateur = Long.valueOf(noIndividuOperateurParam);
		}

		UtilisateurListPersonneView bean = (UtilisateurListPersonneView) session.getAttribute(PERSONNE_CRITERIA_NAME);
		if(	(bean == null) ||
				((action != null) && action.equals(EFFACER_PARAMETER_VALUE)) ) {
			bean = new UtilisateurListPersonneView();
			bean.setTypeRechercheDuNom(TiersCriteriaView.TypeRecherche.EST_EXACTEMENT);
			bean.setTypeTiers(TiersCriteriaView.TypeTiers.PERSONNE_PHYSIQUE);
			bean.setNoIndividuOperateur(noIndividuOperateur);
			UtilisateurView utilisateurView = utilisateurManager.get(noIndividuOperateur);
			bean.setUtilisateurView(utilisateurView);
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
		UtilisateurListPersonneView bean = (UtilisateurListPersonneView) session.getAttribute(PERSONNE_CRITERIA_NAME);
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
						List<TiersIndexedDataView> results = searchTiers(bean);
						mav.addObject(PERSONNE_LIST_ATTRIBUTE_NAME, results);
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
			mav.addObject(PERSONNE_LIST_ATTRIBUTE_NAME, null);
		}

		removeModuleFromSession(request, PERSONNE_CRITERIA_NAME);
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

		UtilisateurListPersonneView bean = (UtilisateurListPersonneView) command;
		HttpSession session = request.getSession();
		session.setAttribute(PERSONNE_CRITERIA_NAME, bean);

		if (request.getParameter(BOUTON_EFFACER) != null) {
			mav.setView(new RedirectView("list-pp-utilisateur.do?action=effacer&noIndividuOperateur=" + bean.getNoIndividuOperateur()));
		} else {
			mav.setView(new RedirectView("list-pp-utilisateur.do"));
		}

		return mav;
	}

	public UtilisateurManager getUtilisateurManager() {
		return utilisateurManager;
	}

	public void setUtilisateurManager(UtilisateurManager utilisateurManager) {
		this.utilisateurManager = utilisateurManager;
	}
}
