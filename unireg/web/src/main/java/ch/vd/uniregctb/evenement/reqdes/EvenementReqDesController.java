package ch.vd.uniregctb.evenement.reqdes;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.beans.propertyeditors.CustomCollectionEditor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.reqdes.EtatTraitement;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityCheck;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.utils.RegDateEditor;

@Controller
@RequestMapping(value = "/evenement/reqdes")
@SessionAttributes(value = {EvenementReqDesController.CRITERIA_NAME, EvenementReqDesController.PAGINATION_NAME})
public class EvenementReqDesController {

	private static final String ACCESS_DENIED_MESSAGE = "Vous ne possédez aucun droit IfoSec de gestion des événements des notaires.";

	public static final String CRITERIA_NAME = "reqdesCriteria";
	public static final String PAGINATION_NAME = "reqdesPagination";

	private static final String TABLE_NAME = "tableUnitesTraitement";
	private static final String NB_UNITES_NAME = "nbUnites";
	private static final String LISTE_UNITES_NAME = "listUnites";

	private static final int PAGE_SIZE = 25;
	private static final String DEFAULT_FIELD = "id";
	private static final WebParamPagination INITIAL_PAGINATION = new WebParamPagination(1, PAGE_SIZE, DEFAULT_FIELD, false);

	private ControllerUtils controllerUtils;
	private TiersMapHelper tiersMapHelper;
	private ReqDesManager manager;

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setManager(ReqDesManager manager) {
		this.manager = manager;
	}

	@ModelAttribute
	public Model referenceData(Model model) throws Exception {
		model.addAttribute("etats", tiersMapHelper.getMapEtatsUniteTraitementReqDes());
		return model;
	}

	@ModelAttribute(value = CRITERIA_NAME)
	public ReqDesCriteriaView initCriteria() {
		final ReqDesCriteriaView criteria = new ReqDesCriteriaView();
		criteria.setEtat(EtatTraitement.EN_ERREUR);
		return criteria;
	}

	@ModelAttribute(value = PAGINATION_NAME)
	public WebParamPagination initPagination() {
		return INITIAL_PAGINATION;
	}

	@InitBinder(value = CRITERIA_NAME)
	protected final void initBinder(HttpServletRequest request, WebDataBinder binder) {
		binder.setValidator(new ReqDesCriteriaViewValidator());

		final Locale locale = request.getLocale();
		final NumberFormat numberFormat = NumberFormat.getInstance(locale);
		numberFormat.setGroupingUsed(true);

		binder.registerCustomEditor(BigDecimal.class, new CustomNumberEditor(BigDecimal.class, numberFormat, true));
		binder.registerCustomEditor(Integer.class, new CustomNumberEditor(Integer.class, numberFormat, true));
		binder.registerCustomEditor(Long.class, new CustomNumberEditor(Long.class, numberFormat, true));
		binder.registerCustomEditor(List.class, new CustomCollectionEditor(List.class, true));
		binder.registerCustomEditor(List.class, new CustomCollectionEditor(Set.class, true));
		binder.registerCustomEditor(boolean.class, new CustomBooleanEditor(true));
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, true, false));
	}

	private void populateModel(Model model,
	                           ReqDesCriteriaView criteres,
	                           ParamPagination pagination,
	                           List<ReqDesUniteTraitementBasicView> liste,
	                           int totalSize) {
		model.addAttribute(CRITERIA_NAME, criteres);
		model.addAttribute(PAGINATION_NAME, pagination);
		model.addAttribute(LISTE_UNITES_NAME, liste);
		model.addAttribute(NB_UNITES_NAME, totalSize);
	}

	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.EVEN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String mainEntryPoint(@ModelAttribute(PAGINATION_NAME) ParamPagination pagination) {
		final String displayTagParameter = controllerUtils.getDisplayTagRequestParametersForPagination(TABLE_NAME, pagination);
		return String.format("redirect:/evenement/reqdes/nav-list.do%s%s",
		                     StringUtils.isNotBlank(displayTagParameter) ? "?" : StringUtils.EMPTY,
		                     StringUtils.trimToEmpty(displayTagParameter));
	}

	@RequestMapping(value = "/nav-list.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.EVEN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String showList(HttpServletRequest request, Model model, @ModelAttribute(CRITERIA_NAME) @Valid ReqDesCriteriaView criteria, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			// on ré-affiche juste la page en effaçant les résultats des recherches précédentes et avec le message d'erreur
			populateModel(model, criteria, INITIAL_PAGINATION, null, 0);
		}
		else {
			// récupération des paramètres de pagination
			final ParamPagination paramPagination = new WebParamPagination(request, TABLE_NAME, PAGE_SIZE, DEFAULT_FIELD, false);
			populateModel(model, criteria, paramPagination, manager.find(criteria, paramPagination), manager.count(criteria));
		}
		return "evenement/reqdes/list";
	}

	@RequestMapping(value = "/effacer.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.EVEN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	protected String effacerFormulaireDeRecherche(Model model) {
		populateModel(model, initCriteria(), INITIAL_PAGINATION, null, 0);
		return "evenement/reqdes/list";
	}


	@RequestMapping(value = "/rechercher.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.EVEN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String doSearch(Model model, @ModelAttribute(CRITERIA_NAME) @Valid ReqDesCriteriaView criteria, BindingResult bindingResult) {
		populateModel(model, criteria, INITIAL_PAGINATION, null, 0);
		if (bindingResult.hasErrors()) {
			// on ré-affiche juste la page en effaçant les résultats des recherches précédentes et avec le message d'erreur
			return "evenement/reqdes/list";
		}
		else {
			// retour à la première page pour les résultats correspondant à la nouvelle rercherhe
			return mainEntryPoint(INITIAL_PAGINATION);
		}
	}
}

