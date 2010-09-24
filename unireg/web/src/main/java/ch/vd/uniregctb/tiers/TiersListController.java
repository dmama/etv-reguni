package ch.vd.uniregctb.tiers;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.TiersCriteria.TypeTiers;
import ch.vd.uniregctb.tiers.manager.TiersListManager;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.web.xt.handler.TabulationHandler;

/**
 * Controller gerant la recherche des tiers.
 *
 */
public class TiersListController extends AbstractTiersListController {

	protected static final Logger LOGGER = Logger.getLogger(TiersListController.class);

	private TiersListManager tiersListManager;

	public final static String NUMERO_PARAMETER_NAME = "numero";
	public final static String NOM_RAISON_PARAMETER_NAME = "nomRaison";
	public final static String LOCALITE_OU_PAYS_PARAMETER_NAME = "localiteOuPays";
	public final static String NO_OFS_FOR_PARAMETER_NAME = "noOfsFor";
	public final static String DATE_NAISSANCE_PARAMETER_NAME = "dateNaissance";
	public final static String NUMERO_ASSURE_SOCIAL_PARAMETER_NAME = "numeroAssureSocial";
	public final static String TYPE_RECHERCHE_PARAMETER_NAME = "typeRecherche";
	public final static String URL_RETOUR_PARAMETER_NAME = "urlRetour";


	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of TiersListController:formBackingObject");
		}

		String numeroParam = request.getParameter(NUMERO_PARAMETER_NAME);
		String nomRaisonParam = request.getParameter(NOM_RAISON_PARAMETER_NAME);
		String localiteOuPaysParam = request.getParameter(LOCALITE_OU_PAYS_PARAMETER_NAME);
		String noOfsForParam = request.getParameter(NO_OFS_FOR_PARAMETER_NAME);
		String dateNaissanceParam = request.getParameter(DATE_NAISSANCE_PARAMETER_NAME);
		RegDate dateNaissance = null;
		if (!StringUtils.isBlank(dateNaissanceParam)) {
			dateNaissance = RegDateHelper.displayStringToRegDate(dateNaissanceParam.trim(), true);
		}
		String numeroAssureSocialParam = request.getParameter(NUMERO_ASSURE_SOCIAL_PARAMETER_NAME);

		HttpSession session = request.getSession();
		String urlRetourParam = request.getParameter(URL_RETOUR_PARAMETER_NAME);
		String typeRechercheParam = request.getParameter(TYPE_RECHERCHE_PARAMETER_NAME);
		TypeTiers typeTiers = null;
		if (!StringUtils.isBlank(typeRechercheParam)) {
			typeTiers = TypeTiers.valueOf(typeRechercheParam);
		}

		TabulationHandler.resetCurrentTabulation(request, "tiersTabs");
		String buttonEffacer = request.getParameter(ACTION_PARAMETER_NAME);
		TiersCriteriaView bean = null;

		if ((buttonEffacer != null) && (buttonEffacer.equals(EFFACER_PARAMETER_VALUE) )) {
			removeModuleFromSession(request, TIERS_CRITERIA_NAME);
			bean = new TiersCriteriaView();
		}
		else {
			bean = (TiersCriteriaView) session.getAttribute(TIERS_CRITERIA_NAME);
		 	if (bean == null) {
				bean = (TiersCriteriaView) super.formBackingObject(request);
				bean.setTypeRechercheDuNom(TiersCriteriaView.TypeRecherche.EST_EXACTEMENT);

				//gestion des droits
				if(!SecurityProvider.isGranted(Role.VISU_ALL)){
					if(!SecurityProvider.isGranted(Role.VISU_LIMITE)){
						throw new AccessDeniedException("vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
					}
					bean.setTypeVisualisation(TiersCriteriaView.TypeVisualisation.LIMITEE);
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("utilisateur avec visualisation limitée");
					}
				}
		 	}
		 	if( urlRetourParam != null) {
				session.setAttribute(URL_RETOUR_SESSION_NAME, urlRetourParam);
				tiersListManager.initFieldsWithParams(	bean, typeTiers, numeroParam, nomRaisonParam, localiteOuPaysParam,
						noOfsForParam, dateNaissance, numeroAssureSocialParam);
			}

		 	session.setAttribute(TIERS_CRITERIA_NAME, bean);
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
		TiersCriteriaView bean = (TiersCriteriaView) session.getAttribute(TIERS_CRITERIA_NAME);
		ModelAndView mav = showFormForList(request, response, errors, model, TIERS_CRITERIA_NAME, TIERS_LIST_ATTRIBUTE_NAME, bean, true);
		mav.addObject(URL_RETOUR_SESSION_NAME, session.getAttribute(URL_RETOUR_SESSION_NAME));
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
		session.setAttribute(TIERS_CRITERIA_NAME, bean);
		if (request.getParameter(BOUTON_EFFACER) != null) {
			mav.setView(new RedirectView("list.do?action=effacer"));
		} else {
			mav.setView(new RedirectView(getSuccessView()));
		}
		return mav;
	}

	public TiersListManager getTiersListManager() {
		return tiersListManager;
	}

	public void setTiersListManager(TiersListManager tiersListManager) {
		this.tiersListManager = tiersListManager;
	}

}
