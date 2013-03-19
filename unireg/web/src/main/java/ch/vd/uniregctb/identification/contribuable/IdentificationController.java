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
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.evenement.identification.contribuable.Demande;
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
public class IdentificationController {

	private static final String ACCESS_DENIED_MESSAGE = "Vous ne possédez aucun droit IfoSec pour la gestion de l'identification de contribuable";
	private static final String ACCESS_DENIED_UNLOCK_MESSAGE = "Vous ne possédez aucun droit IfoSec pour déverouiller un message d'identification de contribuable";
	private static final String ACCESS_DENIED_VISU_MESSAGE = "Vous ne possédez aucun droit IfoSec pour visualiser les messages d'identification de contribuable";
	private IdentificationMessagesStatsManager identificationMessagesStatsManager;
	private IdentificationMapHelper identificationMapHelper;
	private IdentificationMessagesEditManager identificationMessagesEditManager;
	private SecurityProviderInterface securityProvider;
	private IdentificationMessagesListManager identificationMessagesListManager;

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

	@RequestMapping(value = "/tableau-bord/effacer.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_ADMIN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	protected String effacerFormulaireDeRecherche(ModelMap model) throws Exception {
		setUpModelForStats(model);
		final IdentificationMessagesStatsCriteriaView statsCriteriaView = identificationMessagesStatsManager.getView();
		model.put("statsCriteria", statsCriteriaView);
		model.put("statistiques", identificationMessagesStatsManager.calculerStats(statsCriteriaView));
		return "identification/tableau-bord/stats";
	}

	@RequestMapping(value = "/gestion-messages/effacerEnCours.do")
	protected ModelAndView effacerFormulaireDeRechercheEnCours(HttpServletRequest request,ModelMap model) throws Exception {
		IdentificationContribuableListCriteria criteria = identificationMessagesListManager.getView(null,null,null);
		return buildResponseForMessageEnCours(request, criteria, model);
	}

	@RequestMapping(value = "/gestion-messages/effacerTraite.do")
	protected ModelAndView effacerFormulaireDeRechercheTraite(HttpServletRequest request,ModelMap model) throws Exception {
		IdentificationContribuableListCriteria criteria = identificationMessagesListManager.getView(null,null,null);
		return buildReponseForMessageTraite(request, model, criteria);
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
	                                           @ModelAttribute("identificationCriteria") IdentificationContribuableListCriteria criteria, ModelMap model) throws Exception {
		deverouillerMessage(criteria);
		return buildResponseForMessageEnCours(request, criteria, model);
	}

	@RequestMapping(value = {"/gestion-messages/lock.do"}, method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_GEST_BO}, accessDeniedMessage = ACCESS_DENIED_UNLOCK_MESSAGE)
	protected ModelAndView verouillerMessage(HttpServletRequest request,@ModelAttribute("identificationCriteria") IdentificationContribuableListCriteria criteria, ModelMap model) throws Exception {

		verouillerMessage(criteria);
		return buildResponseForMessageEnCours(request, criteria, model);
	}
	@RequestMapping(value = {"/gestion-messages/suspendre.do"}, method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_GEST_BO}, accessDeniedMessage = ACCESS_DENIED_UNLOCK_MESSAGE)
	protected ModelAndView suspendreMessage(HttpServletRequest request,@ModelAttribute("identificationCriteria") IdentificationContribuableListCriteria criteria, ModelMap model) throws Exception {

		identificationMessagesListManager.suspendreIdentificationMessages(criteria);
		return buildResponseForMessageEnCours(request, criteria, model);
	}
	@RequestMapping(value = {"/gestion-messages/resoumettre.do"}, method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_GEST_BO}, accessDeniedMessage = ACCESS_DENIED_UNLOCK_MESSAGE)
	protected ModelAndView reSoumettreMessage(HttpServletRequest request, @ModelAttribute("identificationCriteria") IdentificationContribuableListCriteria criteria, ModelMap model) throws Exception {


		identificationMessagesListManager.reSoumettreIdentificationMessages(criteria);
		return buildResponseForMessageEnCours(request, criteria, model);
	}


	@RequestMapping(value = {"/gestion-messages/demandeEdit.do"}, method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_GEST_BO}, accessDeniedMessage = ACCESS_DENIED_UNLOCK_MESSAGE)
	protected ModelAndView demanderEditionMessage(HttpServletRequest request,
	                                              @ModelAttribute("identificationCriteria") IdentificationContribuableListCriteria identCriteria,
	                                              @RequestParam(value = "id", required = true) Long idMessage,  @RequestParam(value = "source", required = true) String source,ModelMap model) throws Exception {

		if (!identificationMessagesEditManager.isMessageVerouille(idMessage)) {
			return new ModelAndView("redirect:edit.do?id=" + idMessage+"&source="+source);
		}
		else {
			Flash.warning("le message sélectionné est en cours de traitement par un autre utilisateur");
		}
		IdentificationContribuableListCriteria criteria = identificationMessagesListManager.getView(null,null,null);
		if ("enCours".equals(source)) {
			return buildResponseForMessageEnCours(request, criteria, model);
		}
		else{
			return buildResponseForMessageSuspendu(request,criteria,model);
		}

	}

	@RequestMapping(value = {"/gestion-messages/listEnCours.do"}, method = {RequestMethod.POST, RequestMethod.GET})
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_VISU, Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_CELLULE_BO,
			Role.MW_IDENT_CTB_GEST_BO, Role.NCS_IDENT_CTB_CELLULE_BO, Role.LISTE_IS_IDENT_CTB_CELLULE_BO}, accessDeniedMessage = ACCESS_DENIED_VISU_MESSAGE)
	protected ModelAndView listerEncours(HttpServletRequest request,
	                              @ModelAttribute("identificationCriteria") IdentificationContribuableListCriteria criteria,
	                              BindingResult bindingResult,
	                              @RequestParam(value = "keepCriteria", required = false) String keepCriteria,
	                              ModelMap model) throws AdressesResolutionException {

		criteria = manageCriteria(request, criteria, "identificationCriteriaEnCours", keepCriteria);
		return buildResponseForMessageEnCours(request, criteria, model);
	}

	/**Permet de conserver les critères sélectionneées des messages dans la session
	 *
	 * @param request
	 * @param criteria
	 * @param criteriaName
	 * @param keepCriteria
	 */
	private IdentificationContribuableListCriteria manageCriteria(HttpServletRequest request, IdentificationContribuableListCriteria criteria, String criteriaName, String keepCriteria) {
		if (criteria.isEmpty()) {
			IdentificationContribuableListCriteria savedCriteria = (IdentificationContribuableListCriteria) request.getSession().getAttribute(criteriaName);
			if (savedCriteria != null && !savedCriteria.isEmpty() && keepCriteria!=null) {
				criteria = savedCriteria;
			}
		}
		request.getSession().setAttribute(criteriaName,criteria);
		return criteria;
	}

	@RequestMapping(value = {"/gestion-messages/listSuspendu.do"}, method = {RequestMethod.POST, RequestMethod.GET})
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_VISU, Role.MW_IDENT_CTB_ADMIN}, accessDeniedMessage = ACCESS_DENIED_VISU_MESSAGE)
	protected ModelAndView listerSuspendu(HttpServletRequest request,
	                                     @ModelAttribute("identificationCriteria") IdentificationContribuableListCriteria criteria,
	                                     BindingResult bindingResult,
	                                     @RequestParam(value = "keepCriteria", required = false) String keepCriteria,
	                                     ModelMap model) throws AdressesResolutionException {

		criteria = manageCriteria(request, criteria, "identificationCriteriaEnCours", keepCriteria);
		return buildResponseForMessageSuspendu(request, criteria, model);
	}


	@RequestMapping(value = {"/gestion-messages/listEnCoursFromStats.do"}, method = {RequestMethod.POST, RequestMethod.GET})
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_VISU, Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_CELLULE_BO,
			Role.MW_IDENT_CTB_GEST_BO, Role.NCS_IDENT_CTB_CELLULE_BO, Role.LISTE_IS_IDENT_CTB_CELLULE_BO}, accessDeniedMessage = ACCESS_DENIED_VISU_MESSAGE)
	protected ModelAndView listerEnCoursFromStats(HttpServletRequest request,
	                                              @RequestParam(value = "etat", required = true) String etat,
	                                              @RequestParam(value = "periode", required = true) String periode,
	                                              @RequestParam(value = "typeMessage", required = true) String typeMessage,
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
		criteria = manageCriteria(request, criteria, "identificationCriteriaEnCours", null);
		return new ModelAndView("identification/gestion-messages/list", model);
	}

	@RequestMapping(value = {"/gestion-messages/listSuspenduFromStats.do"}, method = {RequestMethod.POST, RequestMethod.GET})
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_VISU, Role.MW_IDENT_CTB_ADMIN}, accessDeniedMessage = ACCESS_DENIED_VISU_MESSAGE)
	protected ModelAndView listerSuspenduFromStats(HttpServletRequest request,
	                                              @RequestParam(value = "etat", required = true) String etat,
	                                              @RequestParam(value = "periode", required = true) String periode,
	                                              @RequestParam(value = "typeMessage", required = true) String typeMessage,
	                                              ModelMap model) throws AdressesResolutionException {

		setUpModelForListMessageSuspendu(model);

		IdentificationContribuableListCriteria criteria = identificationMessagesListManager.getView(typeMessage, periode, etat);
		model.put("identificationCriteria", criteria);
		construireModelMessageSuspendu(request, model, criteria);
		criteria = manageCriteria(request, criteria, "identificationCriteriaEnCours", null);
		return new ModelAndView("identification/gestion-messages/list", model);
	}

	@RequestMapping(value = {"/gestion-messages/listTraite.do"}, method = {RequestMethod.POST, RequestMethod.GET})
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_VISU, Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_CELLULE_BO,
			Role.MW_IDENT_CTB_GEST_BO, Role.NCS_IDENT_CTB_CELLULE_BO, Role.LISTE_IS_IDENT_CTB_CELLULE_BO}, accessDeniedMessage = ACCESS_DENIED_VISU_MESSAGE)
	protected ModelAndView listerTraite(HttpServletRequest request,
	                              @ModelAttribute("identificationCriteria") IdentificationContribuableListCriteria criteria,
	                              BindingResult bindingResult,
	                              @RequestParam(value = "keepCriteria", required = false) String keepCriteria,
	                              ModelMap model) throws AdressesResolutionException {

		criteria = manageCriteria(request, criteria, "identificationCriteriaTraite", keepCriteria);
		return buildReponseForMessageTraite(request, model, criteria);
	}


	@RequestMapping(value = {"/gestion-messages/listTraiteFromStats.do"}, method = {RequestMethod.POST, RequestMethod.GET})
	@SecurityCheck(rolesToCheck = {Role.MW_IDENT_CTB_VISU, Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_CELLULE_BO,
			Role.MW_IDENT_CTB_GEST_BO, Role.NCS_IDENT_CTB_CELLULE_BO, Role.LISTE_IS_IDENT_CTB_CELLULE_BO}, accessDeniedMessage = ACCESS_DENIED_VISU_MESSAGE)
	protected ModelAndView listerTraiteFromStats(HttpServletRequest request,
	                                              @RequestParam(value = "etat", required = true) String etat,
	                                              @RequestParam(value = "periode", required = true) String periode,
	                                              @RequestParam(value = "typeMessage", required = true) String typeMessage,
	                                              ModelMap model) throws AdressesResolutionException {

		setUpModelForListMessageTraites(model);

		IdentificationContribuableListCriteria criteria = identificationMessagesListManager.getView(typeMessage, periode, etat);
		model.put("identificationCriteria", criteria);
		construireModelMessageTraite(request, model, criteria);
		criteria = manageCriteria(request, criteria, "identificationCriteriaTraite", null);
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
	private ModelAndView buildResponseForMessageSuspendu(HttpServletRequest request, IdentificationContribuableListCriteria criteria, ModelMap model) throws AdressesResolutionException {
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
		}else if (SecurityHelper.isAnyGranted(securityProvider, Role.MW_IDENT_CTB_VISU,Role.MW_IDENT_CTB_GEST_BO)) {
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
		model.put("typesMessage", initMapTypeMessageEncours());
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


	@InitBinder
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
	public static void  calculerView(HttpServletRequest request, ModelAndView mav,String source_param) {
		final String source = (String) request.getSession().getAttribute(source_param);
		if ("enCours".equals(source)) {
			mav.setView(new RedirectView("listEnCours.do?keepCriteria=true"));
		}


		if ("suspendu".equals(source)) {
			mav.setView(new RedirectView("listSuspendu.do?keepCriteria=true"));
		}
	}
}
