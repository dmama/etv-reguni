package ch.vd.uniregctb.identification.contribuable;


import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.beans.propertyeditors.CustomCollectionEditor;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.evenement.identification.contribuable.Demande;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableEtatFilter;
import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;
import ch.vd.uniregctb.identification.contribuable.manager.IdentificationMessagesEditManager;
import ch.vd.uniregctb.identification.contribuable.manager.IdentificationMessagesListManager;
import ch.vd.uniregctb.identification.contribuable.manager.IdentificationMessagesStatsManager;
import ch.vd.uniregctb.identification.contribuable.validator.IdentificationMessageValidator;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationContribuableListCriteria;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesResultView;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesStatsCriteriaView;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityCheck;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.utils.RegDateEditor;

@Controller
@RequestMapping(value = "/identification")
@SessionAttributes({"identificationPagination"})
public class IdentificationController {

	public static enum Source {
		enCours,
		suspendu
	}

	private static final String ACCESS_DENIED_MESSAGE = "Vous ne possédez aucun droit IfoSec pour la gestion de l'identification de contribuable";
	private static final String ACCESS_DENIED_ACTION_MESSAGE = "Vous ne possédez aucun droit IfoSec procéder à cette action sur un message d'identification de contribuable";
	private static final String ACCESS_DENIED_UNLOCK_MESSAGE = "Vous ne possédez aucun droit IfoSec pour déverouiller un message d'identification de contribuable";
	private static final String ACCESS_DENIED_VISU_MESSAGE = "Vous ne possédez aucun droit IfoSec pour visualiser les messages d'identification de contribuable";
	private static final String TABLE_NAME = "message";
	private static final int PAGE_SIZE = 25;
	private static final String DEFAULT_FIELD = "id";
	private static final WebParamPagination INITIAL_PAGINATION = new WebParamPagination(1, PAGE_SIZE, DEFAULT_FIELD, false);
	private IdentificationMessagesStatsManager identificationMessagesStatsManager;
	private IdentificationMapHelper identificationMapHelper;
	private IdentificationMessagesEditManager identificationMessagesEditManager;
	private SecurityProviderInterface securityProvider;
	private IdentificationMessagesListManager identificationMessagesListManager;


	protected ControllerUtils controllerUtils;

	private IdentificationMessageValidator validator;

	public void setIdentificationMessagesStatsManager(IdentificationMessagesStatsManager identificationMessagesStatsManager) {
		this.identificationMessagesStatsManager = identificationMessagesStatsManager;
	}

	public void setIdentificationMapHelper(IdentificationMapHelper identificationMapHelper) {
		this.identificationMapHelper = identificationMapHelper;
	}

	public void setIdentificationMessagesEditManager(IdentificationMessagesEditManager identificationMessagesEditManager) {
		this.identificationMessagesEditManager = identificationMessagesEditManager;
	}


	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setIdentificationMessagesListManager(IdentificationMessagesListManager identificationMessagesListManager) {
		this.identificationMessagesListManager = identificationMessagesListManager;
	}


	public void setValidator(IdentificationMessageValidator validator) {
		this.validator = validator;
	}



	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}


	/**
	 *  Crée un objet ParamPagination initial dans la session
	 *
	 *  Appelé par Spring (grâce à l'annotation) si 'identificationPagination' n'existe pas dans la session
	 *
	 * @return l'objet de pagination initial
	 */
	@ModelAttribute("identificationPagination")
	public ParamPagination initIdentificationPagination() {
		return INITIAL_PAGINATION;
	}

	@RequestMapping(value = "/tableau-bord/effacer.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_ADMIN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	protected String effacerFormulaireDeRecherche(ModelMap model) throws Exception {
		setUpModelForStats(model);
		final IdentificationMessagesStatsCriteriaView statsCriteriaView = identificationMessagesStatsManager.getView();
		model.put("statsCriteria", statsCriteriaView);
		model.put("statistiques", identificationMessagesStatsManager.calculerStats(statsCriteriaView));
		return "identification/tableau-bord/stats";
	}

	@RequestMapping(value = "/gestion-messages/effacerEnCours.do",  method = RequestMethod.POST)
	protected ModelAndView effacerFormulaireDeRechercheEnCours(HttpServletRequest request,
	                                                           ModelMap model) throws Exception {
		IdentificationContribuableListCriteria criteria = manageCriteria(request, identificationMessagesListManager.getView(null, null, null), "identificationCriteria", false);
		model.put("identificationPagination",INITIAL_PAGINATION);
		return buildResponseForMessageEnCours(request, criteria, model);
	}

	@RequestMapping(value = "/gestion-messages/effacerTraite.do", method = RequestMethod.POST)
	protected ModelAndView effacerFormulaireDeRechercheTraite(HttpServletRequest request,ModelMap model) throws Exception {
		IdentificationContribuableListCriteria criteria = manageCriteria(request, identificationMessagesListManager.getView(null, null, null), "identificationCriteria", false);
		return buildReponseForMessageTraite(request, model, criteria);
	}

	@RequestMapping(value = "/gestion-messages/effacerSuspendu.do", method = RequestMethod.POST)
	protected ModelAndView effacerFormulaireDeRechercheSuspendu(HttpServletRequest request,ModelMap model) throws Exception {
		IdentificationContribuableListCriteria  criteria = manageCriteria(request, identificationMessagesListManager.getView(null, null, null), "identificationCriteria", false);
		return buildReponseForMessageSuspendu(request, criteria, model);
	}

	@RequestMapping(value = {"/tableau-bord/stats.do"}, method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_ADMIN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	protected String getStats(HttpServletRequest request,
	                          @ModelAttribute("statsCriteria") IdentificationMessagesStatsCriteriaView criteriaInSession,
	                          BindingResult bindingResult,
	                          ModelMap model) throws Exception {

		setUpModelForStats(model);
		if (criteriaInSession.getTypeMessage() == null && criteriaInSession.getPeriodeFiscale() == null) {
			criteriaInSession = identificationMessagesStatsManager.getView();
		}
		model.put("statistiques", identificationMessagesStatsManager.calculerStats(criteriaInSession));
		return "identification/tableau-bord/stats";
	}

	@RequestMapping(value = {"/gestion-messages/unlock.do"}, method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_GEST_BO}, accessDeniedMessage = ACCESS_DENIED_UNLOCK_MESSAGE)
	protected ModelAndView deverouillerMessage(HttpServletRequest request,
	                                           @RequestParam(value = "source", required = true) Source source,
	                                           @ModelAttribute("identificationCriteria") IdentificationContribuableListCriteria criteria,
	                                           ModelMap model) throws Exception {
		deverouillerMessage(criteria);
		return buildModelAndViewFromSource(request,source,criteria,model);
	}

	@RequestMapping(value = {"/gestion-messages/lock.do"}, method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_GEST_BO}, accessDeniedMessage = ACCESS_DENIED_ACTION_MESSAGE)
	protected ModelAndView verouillerMessage(HttpServletRequest request,
	                                         @RequestParam(value = "source", required = true) Source source,
	                                         @ModelAttribute("identificationCriteria") IdentificationContribuableListCriteria criteria,
	                                         ModelMap model) throws Exception {
		verouillerMessage(criteria);
		return buildModelAndViewFromSource(request,source,criteria,model);
	}

	@RequestMapping(value = {"/gestion-messages/suspendre.do"}, method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_GEST_BO}, accessDeniedMessage = ACCESS_DENIED_ACTION_MESSAGE)
	protected ModelAndView suspendreMessage(HttpServletRequest request,
	                                        @RequestParam(value = "source", required = true) Source source,
	                                        @ModelAttribute("identificationCriteria") IdentificationContribuableListCriteria criteria,
	                                        ModelMap model) throws Exception {
		identificationMessagesListManager.suspendreIdentificationMessages(criteria);
		return buildModelAndViewFromSource(request,source,criteria,model);
	}

	@RequestMapping(value = {"/gestion-messages/resoumettre.do"}, method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_GEST_BO}, accessDeniedMessage = ACCESS_DENIED_ACTION_MESSAGE)
	protected ModelAndView reSoumettreMessage(HttpServletRequest request,
	                                          @RequestParam(value = "source", required = true) Source source,
	                                          @ModelAttribute("identificationCriteria") IdentificationContribuableListCriteria criteria,
	                                          ModelMap model) throws Exception {
		identificationMessagesListManager.reSoumettreIdentificationMessages(criteria);
		return buildModelAndViewFromSource(request,source,criteria,model);
	}


	@RequestMapping(value = {"/gestion-messages/demandeEdit.do"}, method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_GEST_BO}, accessDeniedMessage = ACCESS_DENIED_ACTION_MESSAGE)
	protected ModelAndView demanderEditionMessage(HttpServletRequest request,
	                                              @ModelAttribute("identificationCriteria") IdentificationContribuableListCriteria identCriteria,
	                                              @RequestParam(value = "id", required = true) Long idMessage,
	                                              @RequestParam(value = "source", required = true) Source source,
	                                              ModelMap model) throws Exception {

		if (!identificationMessagesEditManager.isMessageVerouille(idMessage)) {
			return new ModelAndView("redirect:edit.do?id=" + idMessage+"&source="+source);
		}
		else {
			Flash.warning("le message sélectionné est en cours de traitement par un autre utilisateur");
		}
		final IdentificationContribuableListCriteria criteria = identificationMessagesListManager.getView(null,null,null);
		return buildResponseFromSource(request, source, criteria, model);
	}

	private ModelAndView buildResponseFromSource(HttpServletRequest request, Source source, IdentificationContribuableListCriteria criteria, ModelMap model) throws AdressesResolutionException {
		if (source == Source.enCours) {
			return buildResponseForMessageEnCours(request, criteria, model);
		}
		else if (source == Source.suspendu) {
			return buildReponseForMessageSuspendu(request, criteria, model);
		}

		throw new IllegalArgumentException("Invalid value for source parameter");
	}

	private ModelAndView buildModelAndViewFromSource(HttpServletRequest request, Source source, IdentificationContribuableListCriteria criteria, ModelMap model) throws AdressesResolutionException {
		if (source == Source.enCours) {
			return new ModelAndView("redirect:/identification/gestion-messages/listEnCours.do");
		}
		else if (source == Source.suspendu) {
			return new ModelAndView("redirect:/identification/gestion-messages/listSuspendu.do");
		}

		throw new IllegalArgumentException("Invalid value for source parameter");
	}


	@RequestMapping(value = "/gestion-messages/listEnCours.do", method =  {RequestMethod.POST, RequestMethod.GET})
	@SecurityCheck(rolesToCheck = {Role.EVEN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	protected String retourSurLaListeEnCours(@ModelAttribute("identificationPagination") ParamPagination paginationInSession ) throws AdresseException {
		// Redirect vers nav-list.do  avec en parametre la pagination en session
		return buildNavListRedirect(paginationInSession,"nav-listEnCours.do");
	}

	@RequestMapping(value = {"/gestion-messages/nav-listEnCours.do"}, method = {RequestMethod.POST, RequestMethod.GET})
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_VISU, Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_CELLULE_BO,
			Role.MW_IDENT_CTB_GEST_BO, Role.NCS_IDENT_CTB_CELLULE_BO, Role.LISTE_IS_IDENT_CTB_CELLULE_BO}, accessDeniedMessage = ACCESS_DENIED_VISU_MESSAGE)
	protected ModelAndView listerEncours(HttpServletRequest request,
	                              @ModelAttribute("identificationCriteria") IdentificationContribuableListCriteria criteria,
	                              BindingResult bindingResult,
	                              @RequestParam(value = "wipeCriteria", required = false) String wipeCriteria,
	                              ModelMap model) throws AdressesResolutionException {

		criteria = manageCriteria(request, criteria, "identificationCriteria", wipeCriteria == null);
		addPaginationToModel(request, model);
		return buildResponseForMessageEnCours(request, criteria, model);
	}

	private void addPaginationToModel(HttpServletRequest request, ModelMap model) {
		final ParamPagination pagination = new WebParamPagination(request, TABLE_NAME, PAGE_SIZE, DEFAULT_FIELD, false);
		model.put("identificationPagination",pagination);
	}

	/**Permet de conserver les critères sélectionneées des messages dans la session
	 *
	 * @param request
	 * @param criteria
	 * @param criteriaName
	 * @param keepCriteria
	 */
	private IdentificationContribuableListCriteria manageCriteria(HttpServletRequest request, IdentificationContribuableListCriteria criteria, String criteriaName, boolean keepCriteria) {
		if (criteria.isEmpty()) {
			IdentificationContribuableListCriteria savedCriteria = (IdentificationContribuableListCriteria) request.getSession().getAttribute(criteriaName);
			if (savedCriteria != null && !savedCriteria.isEmpty() && keepCriteria) {
				criteria = savedCriteria;
			}
		}
		request.getSession().setAttribute(criteriaName,criteria);
		return criteria;
	}

	@RequestMapping(value = "/gestion-messages/listSuspendu.do", method =  {RequestMethod.POST, RequestMethod.GET})
	@SecurityCheck(rolesToCheck = {Role.EVEN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	protected String retourSurLaListeSuspendu(@ModelAttribute("identificationPagination") ParamPagination paginationInSession ) throws AdresseException {
		// Redirect vers nav-list.do  avec en parametre la pagination en session
		return buildNavListRedirect(paginationInSession,"nav-listSuspendu.do");
	}

	@RequestMapping(value = {"/gestion-messages/nav-listSuspendu.do"}, method = {RequestMethod.POST, RequestMethod.GET})
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_GEST_BO, Role.MW_IDENT_CTB_ADMIN}, accessDeniedMessage = ACCESS_DENIED_VISU_MESSAGE)
	protected ModelAndView listerSuspendu(HttpServletRequest request,
	                                     @ModelAttribute("identificationCriteria") IdentificationContribuableListCriteria criteria,
	                                     BindingResult bindingResult,
	                                     @RequestParam(value = "wipeCriteria", required = false) String wipeCriteria,
	                                     ModelMap model) throws AdressesResolutionException {

		criteria = manageCriteria(request, criteria, "identificationCriteria", wipeCriteria == null);
		addPaginationToModel(request, model);
		return buildReponseForMessageSuspendu(request, criteria, model);
	}


	@RequestMapping(value = {"/gestion-messages/listEnCoursFromStats.do"}, method = {RequestMethod.POST, RequestMethod.GET})
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_VISU, Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_CELLULE_BO,
			Role.MW_IDENT_CTB_GEST_BO, Role.NCS_IDENT_CTB_CELLULE_BO, Role.LISTE_IS_IDENT_CTB_CELLULE_BO}, accessDeniedMessage = ACCESS_DENIED_VISU_MESSAGE)
	protected ModelAndView listerEnCoursFromStats(HttpServletRequest request,
	                                              @RequestParam(value = "etat", required = false) IdentificationContribuable.Etat etat,
	                                              @RequestParam(value = "periode", required = false) Integer periode,
	                                              @RequestParam(value = "typeMessage", required = false) String typeMessage,
	                                              ModelMap model) throws AdressesResolutionException {


		if (SecurityHelper.isAnyGranted(securityProvider, Role.MW_IDENT_CTB_ADMIN)) {
			setUpModelForListMessageEnCoursAvecException(model);
		}
		else{
			setUpModelForListMessageEnCours(model);
		}

		IdentificationContribuableListCriteria criteria = identificationMessagesListManager.getView(typeMessage, periode, etat);
		model.put("identificationCriteria", criteria);
		construireModelMessageEnCours(request, model, criteria);
		criteria = manageCriteria(request, criteria, "identificationCriteria", true);
		return new ModelAndView("identification/gestion-messages/list", model);
	}

	@RequestMapping(value = {"/gestion-messages/listSuspenduFromStats.do"}, method = {RequestMethod.POST, RequestMethod.GET})
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_CELLULE_BO, Role.MW_IDENT_CTB_ADMIN}, accessDeniedMessage = ACCESS_DENIED_VISU_MESSAGE)
	protected ModelAndView listerSuspenduFromStats(HttpServletRequest request,
	                                              @RequestParam(value = "etat", required = false) IdentificationContribuable.Etat etat,
	                                              @RequestParam(value = "periode", required = false) Integer periode,
	                                              @RequestParam(value = "typeMessage", required = false) String typeMessage,
	                                              ModelMap model) throws AdressesResolutionException {

		setUpModelForListMessageSuspendu(model);

		IdentificationContribuableListCriteria criteria = identificationMessagesListManager.getView(typeMessage, periode, etat);
		model.put("identificationCriteria", criteria);
		construireModelMessageSuspendu(request, model, criteria);
		criteria = manageCriteria(request, criteria, "identificationCriteria", true);
		return new ModelAndView("identification/gestion-messages/list", model);
	}


	@RequestMapping(value = "/gestion-messages/listTraite.do", method =  {RequestMethod.POST, RequestMethod.GET})
	@SecurityCheck(rolesToCheck = {Role.EVEN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	protected String retourSurLaListeTraite(@ModelAttribute("identificationPagination") ParamPagination paginationInSession ) throws AdresseException {
		// Redirect vers nav-list.do  avec en parametre la pagination en session
		return buildNavListRedirect(paginationInSession,"nav-listTraite.do");
	}
	@RequestMapping(value = {"/gestion-messages/nav-listTraite.do"}, method = {RequestMethod.POST, RequestMethod.GET})
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_VISU, Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_CELLULE_BO,
			Role.MW_IDENT_CTB_GEST_BO, Role.NCS_IDENT_CTB_CELLULE_BO, Role.LISTE_IS_IDENT_CTB_CELLULE_BO}, accessDeniedMessage = ACCESS_DENIED_VISU_MESSAGE)
	protected ModelAndView listerTraite(HttpServletRequest request,
	                              @ModelAttribute("identificationCriteria") IdentificationContribuableListCriteria criteria,
	                              BindingResult bindingResult,
	                              @RequestParam(value = "wipeCriteria", required = false) String wipeCriteria,
	                              ModelMap model) throws AdressesResolutionException {

		criteria = manageCriteria(request, criteria, "identificationCriteria", wipeCriteria == null);
		addPaginationToModel(request, model);
		return buildReponseForMessageTraite(request, model, criteria);
	}


	@RequestMapping(value = {"/gestion-messages/listTraiteFromStats.do"}, method = {RequestMethod.POST, RequestMethod.GET})
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_VISU, Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_CELLULE_BO,
			Role.MW_IDENT_CTB_GEST_BO, Role.NCS_IDENT_CTB_CELLULE_BO, Role.LISTE_IS_IDENT_CTB_CELLULE_BO}, accessDeniedMessage = ACCESS_DENIED_VISU_MESSAGE)
	protected ModelAndView listerTraiteFromStats(HttpServletRequest request,
	                                              @RequestParam(value = "etat", required = false) IdentificationContribuable.Etat etat,
	                                              @RequestParam(value = "periode", required = false) Integer periode,
	                                              @RequestParam(value = "typeMessage", required = false) String typeMessage,
	                                              ModelMap model) throws AdressesResolutionException {

		setUpModelForListMessageTraites(model);

		IdentificationContribuableListCriteria criteria = identificationMessagesListManager.getView(typeMessage, periode, etat);
		model.put("identificationCriteria", criteria);
		construireModelMessageTraite(request, model, criteria);
		criteria = manageCriteria(request, criteria, "identificationCriteria", true);
		return new ModelAndView("identification/gestion-messages/list", model);
	}

	/**Construit le model est retourne le modelAndView de réponse des messages traites
	 *
	 * @param request
	 * @param model
	 * @param criteria
	 * @return
	 * @throws AdressesResolutionException
	 */
	private ModelAndView buildReponseForMessageTraite(HttpServletRequest request, ModelMap model, IdentificationContribuableListCriteria criteria) throws AdressesResolutionException {
		setUpModelForListMessageTraites(model);
		model.put("identificationCriteria", criteria);
		construireModelMessageTraite(request, model, criteria);
		return new ModelAndView("identification/gestion-messages/list", model);
	}

	/**
	 * Construit le model est retourne le modelAndView de réponse des messages en cours
	 * @param request
	 * @param criteria
	 * @param model
	 * @return
	 * @throws AdressesResolutionException
	 */
	private ModelAndView buildResponseForMessageEnCours(HttpServletRequest request, IdentificationContribuableListCriteria criteria, ModelMap model) throws AdressesResolutionException {
		if (SecurityHelper.isAnyGranted(securityProvider, Role.MW_IDENT_CTB_ADMIN)) {
			setUpModelForListMessageEnCoursAvecException(model);
		}
		else{
			setUpModelForListMessageEnCours(model);
		}

		model.put("identificationCriteria", criteria);
		// Récupération de la pagination
		construireModelMessageEnCours(request, model, criteria);
		return new ModelAndView("identification/gestion-messages/list", model);
	}

	/**
	 * Construit le model est retourne le modelAndView de réponse des messagesSuspendu
	 * @param request
	 * @param criteria
	 * @param model
	 * @return
	 * @throws AdressesResolutionException
	 */
	private ModelAndView buildReponseForMessageSuspendu(HttpServletRequest request, IdentificationContribuableListCriteria criteria, ModelMap model) throws AdressesResolutionException {
		setUpModelForListMessageSuspendu(model);
		model.put("identificationCriteria", criteria);
		// Récupération de la pagination
		construireModelMessageSuspendu(request, model, criteria);
		return new ModelAndView("identification/gestion-messages/list", model);
	}

	private void construireModelMessageEnCours(HttpServletRequest request, ModelMap model, IdentificationContribuableListCriteria criteria) throws AdressesResolutionException {
		// Récupération de la pagination
		final WebParamPagination pagination = new WebParamPagination(request, "message", 25);
		final List<IdentificationMessagesResultView> listIdentifications;
		final int nombreElements;

		if (SecurityHelper.isGranted(securityProvider, Role.MW_IDENT_CTB_ADMIN)) {
			listIdentifications = identificationMessagesListManager.find(criteria, pagination, IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES_ET_EN_EXEPTION);
			nombreElements = identificationMessagesListManager.count(criteria, IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES_ET_EN_EXEPTION);
		}
		else if (SecurityHelper.isAnyGranted(securityProvider, Role.MW_IDENT_CTB_VISU,Role.MW_IDENT_CTB_GEST_BO)) {
			listIdentifications = identificationMessagesListManager.find(criteria, pagination, IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES);
			nombreElements = identificationMessagesListManager.count(criteria, IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES);
		}
		else {//Cellule back office
			final TypeDemande types[] = getAllowedTypes();
			listIdentifications = identificationMessagesListManager.findEncoursSeul(criteria, pagination, types);
			nombreElements = identificationMessagesListManager.countEnCoursSeul(criteria, types);
		}

		model.put("identifications", listIdentifications);
		model.put("identificationsSize", nombreElements);
		model.put("messageEnCours", true);
		model.put("messageTraite", false);
		model.put("messageSuspendu", false);
	}

	private void construireModelMessageSuspendu(HttpServletRequest request, ModelMap model, IdentificationContribuableListCriteria criteria) throws AdressesResolutionException {
		// Récupération de la pagination
		final WebParamPagination pagination = new WebParamPagination(request, "message", 25);
		final List<IdentificationMessagesResultView> listIdentifications;
		final int nombreElements;

		listIdentifications = identificationMessagesListManager.find(criteria, pagination, IdentificationContribuableEtatFilter.SEULEMENT_SUSPENDUS);
		nombreElements = identificationMessagesListManager.count(criteria, IdentificationContribuableEtatFilter.SEULEMENT_SUSPENDUS);

		model.put("identifications", listIdentifications);
		model.put("identificationsSize", nombreElements);
		model.put("messageEnCours", false);
		model.put("messageTraite", false);
		model.put("messageSuspendu", true);
	}


	private void construireModelMessageTraite(HttpServletRequest request, ModelMap model, IdentificationContribuableListCriteria criteria) throws AdressesResolutionException {
		// Récupération de la pagination
		final WebParamPagination pagination = new WebParamPagination(request, "message", 25);
		final List<IdentificationMessagesResultView> listIdentifications;
		int nombreElements;

		final TypeDemande types[] = getAllowedTypes();
		listIdentifications = identificationMessagesListManager.find(criteria, pagination, IdentificationContribuableEtatFilter.SEULEMENT_TRAITES, types);
		nombreElements = identificationMessagesListManager.count(criteria, IdentificationContribuableEtatFilter.SEULEMENT_TRAITES, types);

		model.put("identifications", listIdentifications);
		model.put("identificationsSize", nombreElements);
		model.put("messageEnCours", false);
		model.put("messageTraite", true);
		model.put("messageSuspendu", false);
	}

	private void setUpModelForStats(ModelMap model) throws Exception {
		model.put("typesMessage", identificationMapHelper.initMapTypeMessage(IdentificationContribuableEtatFilter.TOUS));
		model.put("periodesFiscales", identificationMapHelper.initMapPeriodeFiscale(IdentificationContribuableEtatFilter.TOUS));
	}

	private void setUpModelForListMessageEnCours(ModelMap model) {
		model.put("typesMessage", initMapTypeMessageEncours());
		model.put("emetteurs", identificationMapHelper.initMapEmetteurId(IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES));
		model.put("priorites", identificationMapHelper.initMapPrioriteEmetteur());
		model.put("periodesFiscales",identificationMapHelper.initMapPeriodeFiscale(IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES));
		model.put("etatsMessage", identificationMapHelper.initMapEtatMessageEnCours());
	}

	private void setUpModelForListMessageEnCoursAvecException(ModelMap model) {
		model.put("typesMessage", initMapTypeMessageEncoursEtException());
		model.put("emetteurs", identificationMapHelper.initMapEmetteurId(IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES_ET_EN_EXEPTION));
		model.put("priorites", identificationMapHelper.initMapPrioriteEmetteur());
		model.put("periodesFiscales",identificationMapHelper.initMapPeriodeFiscale(IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES_ET_EN_EXEPTION));
		model.put("etatsMessage", identificationMapHelper.initMapEtatMessageEnCoursEtException());
	}

	private void setUpModelForListMessageSuspendu(ModelMap model) {
		model.put("typesMessage",initMapTypeMessageSuspendu());
		model.put("emetteurs", identificationMapHelper.initMapEmetteurId(IdentificationContribuableEtatFilter.SEULEMENT_SUSPENDUS));
		model.put("priorites", identificationMapHelper.initMapPrioriteEmetteur());
		model.put("periodesFiscales", identificationMapHelper.initMapPeriodeFiscale(IdentificationContribuableEtatFilter.SEULEMENT_SUSPENDUS));
		model.put("etatsMessage", identificationMapHelper.initMapEtatMessageSuspendu());
	}

	private void setUpModelForListMessageTraites(ModelMap model) {
		model.put("typesMessage", initMapTypeMessageTraite());
		model.put("periodesFiscales", identificationMapHelper.initMapPeriodeFiscale(IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES));
		model.put("emetteurs",identificationMapHelper.initMapEmetteurId(IdentificationContribuableEtatFilter.SEULEMENT_TRAITES));
		model.put("priorites", identificationMapHelper.initMapPrioriteEmetteur());
		model.put("etatsMessage", identificationMapHelper.initMapEtatMessageArchive());
		model.put("traitementUsers", identificationMapHelper.initMapUser());
	}

	/**
	 * Retourne les types de demandes autorisés par rapports aux droits du principal courant
	 *
	 * @return un tableau (potentiellement vide...) des types autorisés
	 */
	protected TypeDemande[] getAllowedTypes() {
		final Set<TypeDemande> types = new HashSet<>();
		if (SecurityHelper.isAnyGranted(securityProvider, Role.MW_IDENT_CTB_CELLULE_BO, Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_VISU, Role.MW_IDENT_CTB_GEST_BO)) {
			types.add(TypeDemande.MELDEWESEN);
			types.add(TypeDemande.NCS);
			types.add(TypeDemande.IMPOT_SOURCE);
		}
		if (SecurityHelper.isGranted(securityProvider, Role.NCS_IDENT_CTB_CELLULE_BO)) {
			types.add(TypeDemande.NCS);
		}
		if (SecurityHelper.isGranted(securityProvider, Role.LISTE_IS_IDENT_CTB_CELLULE_BO)) {
			types.add(TypeDemande.IMPOT_SOURCE);
		}

		if (types.size() == 0) {
			// aucun droit... plutôt que tous
			return new TypeDemande[]{null};
		}
		else {
			return types.toArray(new TypeDemande[types.size()]);
		}
	}

	public boolean areUsed(String parametreEtat, String parametrePeriode, String parametreTypeMessage) {
		return (parametreEtat != null) || (parametrePeriode != null) || (parametreTypeMessage != null);
	}



	protected Map<String, String> initMapEmetteurIdMessageEncours() {
		return identificationMapHelper.initMapEmetteurId(IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES);
	}

	protected Map<String, String> initMapTypeMessageEncours() {
		final TypeDemande[] types = getAllowedTypes();
		return identificationMapHelper.initMapTypeMessage(IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES, types);
	}

	protected Map<String, String> initMapTypeMessageEncoursEtException() {
		final TypeDemande[] types = getAllowedTypes();
		return identificationMapHelper.initMapTypeMessage(IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES_ET_EN_EXEPTION, types);
	}

	protected Map<String, String> initMapTypeMessageSuspendu() {
		final TypeDemande[] types = getAllowedTypes();
		return identificationMapHelper.initMapTypeMessage(IdentificationContribuableEtatFilter.SEULEMENT_SUSPENDUS, types);
	}

	protected Map<Integer, String> initMapPeriodeFiscaleMessageEncours() {
		return identificationMapHelper.initMapPeriodeFiscale(IdentificationContribuableEtatFilter.SEULEMENT_NON_TRAITES);
	}

	protected Map<Demande.PrioriteEmetteur, String> initMapPrioriteEmetteurMessageEncours() {
		return identificationMapHelper.initMapPrioriteEmetteur();
	}

	protected Map<String, String> initMapEmetteurIdTraite() {
		return identificationMapHelper.initMapEmetteurId(IdentificationContribuableEtatFilter.SEULEMENT_TRAITES);
	}

	protected Map<String, String> initMapTypeMessageTraite() {
		final TypeDemande[] types = getAllowedTypes();
		return identificationMapHelper.initMapTypeMessage(IdentificationContribuableEtatFilter.SEULEMENT_TRAITES, types);
	}

	protected Map<Integer, String> initMapPeriodeFiscaleTraite() {
		return identificationMapHelper.initMapPeriodeFiscale(IdentificationContribuableEtatFilter.SEULEMENT_TRAITES);
	}

	protected Map<Demande.PrioriteEmetteur, String> initMapPrioriteEmetteurTraite() {
		return identificationMapHelper.initMapPrioriteEmetteur();
	}

	@InitBinder("identificationCriteria")
	protected void initBinder(HttpServletRequest request, WebDataBinder binder) {
		binder.setValidator(validator);
		Locale locale = request.getLocale();
		SimpleDateFormat sdf = new SimpleDateFormat(DateHelper.DATE_FORMAT_DISPLAY, locale);
		sdf.setLenient(false);
		binder.registerCustomEditor(Date.class, new CustomDateEditor(sdf, true));
		NumberFormat numberFormat = NumberFormat.getInstance(locale);
		numberFormat.setGroupingUsed(true);
		binder.registerCustomEditor(BigDecimal.class, new CustomNumberEditor(BigDecimal.class, numberFormat, true));
		binder.registerCustomEditor(Integer.class, new CustomNumberEditor(Integer.class, numberFormat, true));
		binder.registerCustomEditor(Long.class, new CustomNumberEditor(Long.class, numberFormat, true));
		binder.registerCustomEditor(List.class, new CustomCollectionEditor(List.class, true));
		binder.registerCustomEditor(List.class, new CustomCollectionEditor(Set.class, true));
		binder.registerCustomEditor(boolean.class, new CustomBooleanEditor(true));
		binder.registerCustomEditor(RegDate.class, "dateNaissance", new RegDateEditor(true, true, false));
	}

	private void deverouillerMessage(IdentificationContribuableListCriteria criteria) throws Exception {
		final Long[] tabIdsMessages = criteria.getTabIdsMessages();
		if (tabIdsMessages != null) {
			for (int i = 0; i < tabIdsMessages.length; i++) {
				identificationMessagesEditManager.deVerouillerMessage(tabIdsMessages[i], true);
			}
		}
	}

	private void verouillerMessage(IdentificationContribuableListCriteria criteria) throws Exception {
		final Long[] tabIdsMessages = criteria.getTabIdsMessages();
		if (tabIdsMessages != null) {
			for (int i = 0; i < tabIdsMessages.length; i++) {
				identificationMessagesEditManager.verouillerMessage(tabIdsMessages[i]);
			}
		}
	}

	public static void calculerView(HttpServletRequest request, ModelAndView mav, String source_param) {
		final String source = (String) request.getSession().getAttribute(source_param);
		final Source src = Source.valueOf(source);
		switch (src) {
			case enCours:
				mav.setView(new RedirectView("listEnCours.do?keepCriteria=true"));
				break;
			case suspendu:
				mav.setView(new RedirectView("listSuspendu.do?keepCriteria=true"));
				break;
		}
	}

	private String buildNavListRedirect(ParamPagination pagination,String action) {
		return buildNavListRedirect(pagination, TABLE_NAME, String.format("/identification/gestion-messages/%s",action));
	}


	protected String buildNavListRedirect(ParamPagination pagination, final String tableName, final String navListPath) {
		String displayTagParameter = controllerUtils.getDisplayTagRequestParametersForPagination(tableName, pagination);
		if (displayTagParameter != null) {
			displayTagParameter = "?" + displayTagParameter;
		}
		return ("redirect:" + navListPath + displayTagParameter);
	}

}
