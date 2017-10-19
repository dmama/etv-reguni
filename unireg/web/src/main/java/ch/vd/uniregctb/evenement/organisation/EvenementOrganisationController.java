package ch.vd.uniregctb.evenement.organisation;

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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.pagination.ParamPagination;
import ch.vd.uniregctb.common.pagination.WebParamPagination;
import ch.vd.uniregctb.evenement.common.AbstractEvenementCivilController;
import ch.vd.uniregctb.evenement.common.AjaxResponseMessage;
import ch.vd.uniregctb.evenement.organisation.manager.EvenementOrganisationManager;
import ch.vd.uniregctb.evenement.organisation.view.EvenementOrganisationCriteriaView;
import ch.vd.uniregctb.evenement.organisation.view.EvenementOrganisationDetailView;
import ch.vd.uniregctb.evenement.organisation.view.EvenementOrganisationElementListeRechercheView;
import ch.vd.uniregctb.evenement.organisation.view.EvenementOrganisationSummaryView;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityCheck;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;


@Controller
@RequestMapping("/evenement/organisation")
@SessionAttributes({"evenementOrganisationCriteria", "evenementOrganisationPagination"})
public class EvenementOrganisationController extends AbstractEvenementCivilController {

	private static final String TABLE_NAME = "tableEvtsOrganisation";
	private static final int PAGE_SIZE = 25;
	private static final String DEFAULT_FIELD = "id";
	private static final String ACCESS_DENIED_MESSAGE = "Vous ne possédez aucun droit IfoSec de gestion des événements organisation";
	private static final WebParamPagination INITIAL_PAGINATION = new WebParamPagination(1, PAGE_SIZE, DEFAULT_FIELD, false);

	private TiersMapHelper tiersMapHelper;
	private EvenementOrganisationManager manager;
	private Validator validator;
	private EvenementOrganisationCappingLevelProvider cappingLevelProvider;

	@SuppressWarnings("UnusedDeclaration")
	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setManager(EvenementOrganisationManager manager) {
		this.manager = manager;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setCappingLevelProvider(EvenementOrganisationCappingSwitch cappingLevelProvider) {
		this.cappingLevelProvider = cappingLevelProvider;
	}

	@InitBinder("evenementOrganisationCriteria")
	protected final void initBinder(HttpServletRequest request, WebDataBinder binder) {
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
	}

	@ModelAttribute
	protected ModelMap referenceData(ModelMap model) throws Exception {
		model.put("typesRechercheNom", tiersMapHelper.getMapTypeRechercheNom());
		model.put("typesEvenementOrganisation", tiersMapHelper.getMapTypeEvenementOrganisation());
		model.put("etatsEvenement", tiersMapHelper.getMapEtatsEvenementOrganisation());
		model.put("formesJuridiques", tiersMapHelper.getMapFormesJuridiquesEntreprise());
		model.put("capping", cappingLevelProvider.getNiveauCapping());
		return model;
	}

	/**
	 *  Crée un objet EvenementOrganisationCriteriaView avec les valeurs par défaut de recherche dans la session.
	 *
	 *  Appelé par Spring (grâce à l'annotation) si 'evenementOrganisationCriteria' n'existe pas dans la session
	 *
	 * @return un nouvel objet initialisé avec les valeur par défaut
	 */
	@ModelAttribute("evenementOrganisationCriteria")
	public EvenementOrganisationCriteriaView initEvenementOrganisationCriteria() {
		EvenementOrganisationCriteriaView criteria =  new EvenementOrganisationCriteriaView();
		criteria.setTypeRechercheDuNom(EvenementOrganisationCriteria.TypeRechercheDuNom.EST_EXACTEMENT);
		criteria.setEtat(EtatEvenementOrganisation.A_VERIFIER);
		return criteria;
	}

	/**
	 *  Crée un objet ParamPagination initial dans la session
	 *
	 *  Appelé par Spring (grâce à l'annotation) si 'evenementPagination' n'existe pas dans la session
	 *
	 * @return l'objet de pagination initial
	 */
	@ModelAttribute("evenementOrganisationPagination")
	public ParamPagination initEvenementPagination() {
		return INITIAL_PAGINATION;
	}

	@RequestMapping(value = "/effacer.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.EVEN_PM}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public ModelAndView effacerFormulaireDeRecherche(ModelMap model) throws AdresseException {
		EvenementOrganisationCriteriaView criteria = initEvenementOrganisationCriteria();
		populateModel(model, criteria, INITIAL_PAGINATION, manager.find(criteria, INITIAL_PAGINATION), manager.count(criteria));
		return new ModelAndView("evenement/organisation/list", model);
	}

	@RequestMapping(value = "/rechercher.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.EVEN_PM}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String rechercher(@ModelAttribute("evenementOrganisationCriteria") @Valid EvenementOrganisationCriteriaView criteriaInSession,
	                            BindingResult bindingResult,
	                            ModelMap model ) throws AdresseException {
		// Stockage des nouveau critère de recherche dans la session
		// La recherche en elle meme est faite dans nav-list.do
		populateModel(model, criteriaInSession,	INITIAL_PAGINATION,	null, 0);
		if (bindingResult.hasErrors()) {
			return "evenement/organisation/list";
		}
		// Redirect vers nav-list.do  avec en parametre une pagination reinitialisée
		return buildNavListRedirect(INITIAL_PAGINATION);
	}

	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.EVEN_PM}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String retourSurLaListe(@ModelAttribute("evenementOrganisationPagination") ParamPagination paginationInSession) throws AdresseException 	{
		// Redirect vers nav-list.do  avec en parametre la pagination en session
		return buildNavListRedirect(paginationInSession);
	}

	@RequestMapping(value = "/nav-list.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.EVEN_PM}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public ModelAndView navigationDansLaListe(HttpServletRequest request,
	                                          @ModelAttribute("evenementOrganisationCriteria") @Valid EvenementOrganisationCriteriaView criteriaInSession,
	                                          BindingResult bindingResult,
	                                          ModelMap model) throws AdresseException 	{
		if (bindingResult.hasErrors() ) {
			// L'utilisateur a soumis un formulaire incorrect
			populateModel(model, criteriaInSession, INITIAL_PAGINATION, null, 0);
		} else {
			// On recupère les paramètres de pagination en request pour les sauver en session
			// On recupère les données correspondant au formulaire de recherche
			final ParamPagination pagination = new WebParamPagination(request, TABLE_NAME, PAGE_SIZE, DEFAULT_FIELD, false);
			populateModel(model,
			              criteriaInSession,
			              pagination,
			              manager.find(criteriaInSession, pagination),
			              manager.count(criteriaInSession));

		}
		return new ModelAndView("evenement/organisation/list", model);
	}

	private String buildNavListRedirect(ParamPagination pagination) {
		return buildNavListRedirect(pagination, TABLE_NAME, "/evenement/organisation/nav-list.do");
	}

	private void populateModel(ModelMap model,
	                           final EvenementOrganisationCriteriaView evenementOrganisationCriteria,
	                           ParamPagination evenementPagination,
	                           @Nullable List<EvenementOrganisationElementListeRechercheView> listEvenementElementListeRecherches,
	                           int listEvenementsSize) {
		model.put("evenementOrganisationCriteria", evenementOrganisationCriteria);
		model.put("evenementOrganisationPagination", evenementPagination);
		model.put("listEvenementsOrganisation", listEvenementElementListeRecherches);
		model.put("listEvenementsOrganisationSize", listEvenementsSize);
	}

	@RequestMapping(value = {"/visu.do"}, method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.EVEN_PM}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public ModelAndView onGetEvenementOrganisation(@RequestParam("id") Long id) throws AdresseException {
		final EvenementOrganisationDetailView visuView = manager.get(id);
		visuView.setEmbedded(false);
		return new ModelAndView ("evenement/organisation/visu", "command", visuView);
	}

	@RequestMapping(value = {"/detail.do"}, method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.EVEN_PM}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public ModelAndView onGetEvenementOrganisationDetail(@RequestParam("id") Long id) throws AdresseException {
		final EvenementOrganisationDetailView visuView = manager.get(id);
		visuView.setEmbedded(true);
		return new ModelAndView ("evenement/organisation/detail", "command", visuView);
	}

	@RequestMapping(value = {"/summary.do"}, method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.EVEN_PM}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public ModelAndView onGetEvenementOrganisationSummary(@RequestParam("id") Long id) throws AdresseException {
		return new ModelAndView ("evenement/organisation/summary", "command", manager.getSummary(id));
	}

	@RequestMapping(value = {"/forcer.do"}, method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.EVEN_PM}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String onForcerEvenementOrganisation(@RequestParam("id") Long id) throws AdresseException {
		manager.forceEvenement(id);
		return "redirect:/evenement/organisation/visu.do?id=" + id;
	}

	@RequestMapping(value = {"/forcerVersListe.do"}, method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.EVEN_PM}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	@ResponseBody()
	public AjaxResponseMessage onForcerEvenementOrganisationVersListe(@RequestParam("id") Long id) throws AdresseException {
		manager.forceEvenement(id);
		return new AjaxResponseMessage(true, null, id);
	}

	@RequestMapping(value = {"/recycler.do"}, method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.EVEN_PM}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String onRecyclerEvenementOrganisation(@RequestParam("id")  Long id) throws AdresseException, EvenementOrganisationException {
		boolean recycle = manager.recycleEvenementOrganisation(id);
		if (recycle) {
			Flash.message("Événement recyclé");
		} else {
			Flash.warning("Demande prise en compte, l'événement est en attente de recyclage");
		}
		return "redirect:/evenement/organisation/visu.do?id=" + id;
	}

	@RequestMapping(value = {"/recyclerVersListe.do"}, method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.EVEN_PM}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	@ResponseBody()
	public AjaxResponseMessage onRecyclerEvenementOrganisationRetourListe(@RequestParam("id")  Long id) throws AdresseException, EvenementOrganisationException {
		final EvenementOrganisationSummaryView summary = manager.getSummary(id);
		boolean recycle = manager.recycleEvenementOrganisation(id);
		String message;
		if (recycle) {
			message = String.format("Événement n°%d recyclé", summary.getNoEvenement());
		} else {
			message = String.format("Demande prise en compte, l'événement n°%d est en attente de recyclage", summary.getNoEvenement());
		}
		return new AjaxResponseMessage(true, message, id);
	}

	@RequestMapping(value = {"/creer-entreprise.do"}, method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.EVEN_PM}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String onCreerEntreprisePourEvenementOrganisation(@RequestParam("id")  Long id) throws AdresseException, EvenementOrganisationException {

		String errorMessage = "";
		Entreprise entreprise = null;
		try {
			entreprise = manager.creerEntreprisePourEvenementOrganisation(id);
		} catch (Exception e) {
			errorMessage = e.getMessage();
			Audit.error(id, e);
		}

		if (entreprise != null) {
			Flash.message(String.format("Entreprise crée avec le numéro %s", entreprise.getNumero()));
		} else {
			Flash.error(String.format("L'entreprise n'a pu être créée. %s", errorMessage));
		}
		return "redirect:/evenement/organisation/visu.do?id=" + id;
	}

	@RequestMapping(value = {"/creer-entrepriseVersListe.do"}, method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.EVEN_PM}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	@ResponseBody()
	public AjaxResponseMessage onCreerEntreprisePourEvenementOrganisationVersListe(@RequestParam("id")  Long id) throws AdresseException, EvenementOrganisationException {

		String errorMessage = "";
		Entreprise entreprise = null;
		try {
			entreprise = manager.creerEntreprisePourEvenementOrganisation(id);
		} catch (Exception e) {
			errorMessage = e.getMessage();
			Audit.error(id, e);
		}
		String message;
		if (entreprise != null) {
			message = String.format("Entreprise crée avec le numéro %s", entreprise.getNumero());
		} else {
			message = String.format("L'entreprise n'a pu être créée. %s", errorMessage);
		}
		return new AjaxResponseMessage(true, message, id);
	}
}

