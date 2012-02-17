package ch.vd.uniregctb.evenement;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.beans.propertyeditors.CustomCollectionEditor;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPPCriteria;
import ch.vd.uniregctb.evenement.manager.EvenementManager;
import ch.vd.uniregctb.evenement.view.EvenementCivilView;
import ch.vd.uniregctb.evenement.view.EvenementCriteriaView;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityCheck;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.type.EtatEvenementCivil;


@Controller
@RequestMapping("/evenement")
@SessionAttributes({"evenementCriteria", "evenementPagination"})
public class EvenementController  {

	private static final String TABLE_NAME = "tableEvts";
	private static final int PAGE_SIZE = 25;
	private static final String DEFAULT_FIELD = "id";
	private static final String ACCESS_DENIED_MESSAGE = "Vous ne possédez aucun droit IfoSec de gestion des événements civils";
	private static final WebParamPagination INITIAL_PAGINATION = new WebParamPagination(1, PAGE_SIZE, DEFAULT_FIELD, false);

	private TiersMapHelper tiersMapHelper;
	public TiersMapHelper getTiersMapHelper() {
		return tiersMapHelper;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	private EvenementManager evenementManager;
	public EvenementManager getEvenementManager() {
		return evenementManager;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setEvenementManager(EvenementManager evenementManager) {
		this.evenementManager = evenementManager;
	}

	private  Validator validator;
	public Validator getValidator() {
		return validator;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setValidator(Validator validator) {
		this.validator = validator;
	}


	@InitBinder("evenementCriteria")
	protected final void initBinder(HttpServletRequest request, WebDataBinder binder) {
		binder.setValidator(getValidator());
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
	}

	@ModelAttribute
	protected ModelMap referenceData(ModelMap model) throws Exception {
		model.put("typesRechercheNom", getTiersMapHelper().getMapTypeRechercheNom());
		model.put("typesEvenement", getTiersMapHelper().getMapTypeEvenementCivil());
		model.put("etatsEvenement", getTiersMapHelper().getMapEtatsEvenementCivil());
		return model;
	}

	/**
	 *  Crée un objet EvenementCriteriaView avec les valeurs par défaut de recherche dans la session.
	 *
	 *  Appelé par Spring (grâce à l'annotation) si 'evenementCriteria' n'existe pas dans la session
	 *
	 * @return un nouvel objet initialisé avec les valeur par défaut
	 */
	@ModelAttribute("evenementCriteria")
	public EvenementCriteriaView initEvenementCriteria() {
		EvenementCriteriaView criteria =  new EvenementCriteriaView();
		criteria.setTypeRechercheDuNom(EvenementCivilRegPPCriteria.TypeRechercheDuNom.EST_EXACTEMENT);
		criteria.setEtat(EtatEvenementCivil.A_VERIFIER);
		return criteria;
	}

	/**
	 *  Crée un objet ParamPagination initial dans la session
	 *
	 *  Appelé par Spring (grâce à l'annotation) si 'evenementPagination' n'existe pas dans la session
	 *
	 * @return l'objet de pagination initial
	 */
	@ModelAttribute("evenementPagination")
	public ParamPagination initEvenementPagination() {
		return INITIAL_PAGINATION;
	}

	@RequestMapping(value = "/nav-list.do", method = RequestMethod.GET, params = "effacer")
	@SecurityCheck(rolesToCheck = {Role.EVEN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	protected ModelAndView effacerFormulaireDeRecherche(ModelMap model) {
		populateModel(model, initEvenementCriteria(), INITIAL_PAGINATION, null, 0);
		return new ModelAndView("evenement/list", model);
	}

	@RequestMapping(value = "/nav-list.do", method = RequestMethod.GET, params = "rechercher")
	@SecurityCheck(rolesToCheck = {Role.EVEN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	protected ModelAndView rechercher(@ModelAttribute("evenementCriteria") @Valid EvenementCriteriaView criteriaInSession,
	                                  BindingResult bindingResult,
	                                  ModelMap model ) throws AdresseException {
		if (bindingResult.hasErrors() ) {
			// L'utilisateur a soumis un formulaire incorrect
			populateModel(model, criteriaInSession, INITIAL_PAGINATION, null, 0);
		} else {
			// on recherche les evenements suivant les critères du formulaire de recherche, et on réinitialise la pagination
			populateModel(model,
					criteriaInSession,
					INITIAL_PAGINATION,
					getEvenementManager().find(criteriaInSession, INITIAL_PAGINATION),
					getEvenementManager().count(criteriaInSession));

		}
		return new ModelAndView("evenement/list", model);
	}

	@RequestMapping(value = "/nav-list.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.EVEN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	protected ModelAndView navigationDansLaListe(HttpServletRequest request,
	                                         @ModelAttribute("evenementCriteria") @Valid EvenementCriteriaView criteriaInSession,
	                                         BindingResult bindingResult,
	                                         ModelMap model ) throws AdresseException 	{
		if (bindingResult.hasErrors() ) {
			// L'utilisateur a soumis un formulaire incorrect
			populateModel(model, criteriaInSession, INITIAL_PAGINATION, null, 0);
		} else {
			// L'utilisateur navigue dans la liste, il faut mettre à jour l'objet de pagination
			final ParamPagination pagination = new WebParamPagination(request, TABLE_NAME, PAGE_SIZE, DEFAULT_FIELD, false);
			populateModel(model,
					criteriaInSession,
					pagination ,
					getEvenementManager().find(criteriaInSession, pagination ),
					getEvenementManager().count(criteriaInSession));

		}
		return new ModelAndView("evenement/list", model);
	}

	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.EVEN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	protected String retourSurLaListe(@ModelAttribute("evenementPagination") ParamPagination paginationInSession ) throws AdresseException 	{
		String displayTagParameter = ControllerUtils.getDisplayTagRequestParametersForPagination(TABLE_NAME, paginationInSession);
		if (displayTagParameter != null) {
			displayTagParameter = "?" + displayTagParameter;
		}
		return "redirect:/evenement/nav-list.do" + displayTagParameter;
	}

	private void populateModel(ModelMap model,
	                           final EvenementCriteriaView evenementCriteria,
	                           ParamPagination evenementPagination,
	                           @Nullable List<EvenementCivilView> listEvenements,
	                           int listEvenementsSize ) {
		model.put("evenementCriteria", evenementCriteria);
		model.put("evenementPagination", evenementPagination);
		model.put("listEvenements", listEvenements);
		model.put("listEvenementsSize", listEvenementsSize);
	}

	@RequestMapping(value = {"/visu.do"}, method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.EVEN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	protected ModelAndView onGetEvenementCivil(@RequestParam("id") Long id) throws AdresseException {
		return new ModelAndView ("evenement/visu", "command", getEvenementManager().get(id));
	}

	@RequestMapping(value = {"/forcer.do"}, method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.EVEN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	protected String onForcerEvenementCivil(@RequestParam("id") Long id) throws AdresseException {
		getEvenementManager().forceEtatTraite(id);
		return "redirect:/evenement/visu.do?id=" + id;
	}

	@RequestMapping(value = {"/recycler.do"}, method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.EVEN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	protected String onRecyclerEvenementCivil(@RequestParam("id")  Long id) throws AdresseException {
		getEvenementManager().traiteEvenementCivil(id);
		return "redirect:/evenement/visu.do?id=" + id;
	}
}

