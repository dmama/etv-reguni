package ch.vd.uniregctb.separation;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.indexer.EmptySearchCriteriaException;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.TooManyResultsIndexerException;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.separation.manager.SeparationRecapManager;
import ch.vd.uniregctb.separation.view.SeparationRecapView;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersIndexedDataView;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.validator.TiersCriteriaValidator;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.utils.RegDateEditor;
import ch.vd.uniregctb.utils.WebContextUtils;

@Controller
@RequestMapping("/separation")
public class SeparationController {

	private static final Logger LOGGER = LoggerFactory.getLogger(SeparationController.class);

	private static final String CRITERIA_SESSION_NAME = "SeparationCriteria";

	private ControllerUtils controllerUtils;
	private TiersService tiersService;
	private SeparationRecapManager separationRecapManager;
	private Validator separationRecapValidator;
	private MessageSource messageSource;
	private TiersMapHelper tiersMapHelper;

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setSeparationRecapManager(SeparationRecapManager separationRecapManager) {
		this.separationRecapManager = separationRecapManager;
	}

	public void setSeparationRecapValidator(Validator separationRecapValidator) {
		this.separationRecapValidator = separationRecapValidator;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	private static TiersCriteriaView getCriteriaFromSessionOrNew(HttpSession session) {
		return Optional.of(session)
				.map(s -> s.getAttribute(CRITERIA_SESSION_NAME))
				.map(TiersCriteriaView.class::cast)
				.orElseGet(TiersCriteriaView::new);
	}

	private static void fillMandatorySearchCriteria(TiersCriteriaView view) {
		view.setTypeTiersImperatif(TiersCriteria.TypeTiers.MENAGE_COMMUN);
	}

	private List<TiersIndexedDataView> searchTiers(TiersCriteriaView criteria) {
		return tiersService.search(criteria.asCore()).stream()
				.map(TiersIndexedDataView::new)
				.collect(Collectors.toList());
	}

	@InitBinder(value = "searchCommand")
	public void initSearchCommandBinder(WebDataBinder binder) {
		binder.setValidator(new TiersCriteriaValidator());
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, true, false, RegDateHelper.StringFormat.DISPLAY));
	}

	@InitBinder(value = "separationCommand")
	public void initCommandBinder(WebDataBinder binder) {
		binder.setValidator(separationRecapValidator);
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
	}

	@RequestMapping(value = "/reset-search.do", method = RequestMethod.GET)
	public String resetSearchCriteria(HttpSession session) {
		session.removeAttribute(CRITERIA_SESSION_NAME);
		return "redirect:list.do";
	}

	@RequestMapping(value = "list.do", method = RequestMethod.GET)
	public String showList(Model model, HttpSession session) {
		final TiersCriteriaView view = getCriteriaFromSessionOrNew(session);
		fillMandatorySearchCriteria(view);

		String searchError = null;
		try {
			final List<TiersIndexedDataView> results = searchTiers(view).stream()
					.filter(found -> separationRecapManager.isAvecForFiscalPrincipalActif(found.getNumero()))
					.collect(Collectors.toList());
			model.addAttribute("list", results);
		}
		catch (EmptySearchCriteriaException e) {
			// rien à faire, aucun résultat, c'est tout...
		}
		catch (TooManyResultsIndexerException e) {
			LOGGER.error("Exception dans l'indexer: " + e.getMessage(), e);
			if (e.getNbResults() > 0) {
				searchError = messageSource.getMessage("error.preciser.recherche.trouves", new Object[] {String.valueOf(e.getNbResults())}, WebContextUtils.getDefaultLocale());
			}
			else {
				searchError = messageSource.getMessage("error.preciser.recherche", null, WebContextUtils.getDefaultLocale());
			}
		}
		catch (IndexerException e) {
			LOGGER.error("Exception dans l'indexer: " + e.getMessage(), e);
			searchError = messageSource.getMessage("error.recherche", null, WebContextUtils.getDefaultLocale());
		}
		model.addAttribute("searchError", searchError);

		return showList(model, view);
	}

	private String showList(Model model, TiersCriteriaView criteria) {
		model.addAttribute("typesRechercheNom", tiersMapHelper.getMapTypeRechercheNom());
		model.addAttribute("searchCommand", criteria);
		return "tiers/edition/separation/list";
	}

	@RequestMapping(value = "/list.do", method = RequestMethod.POST)
	public String doSearch(Model model,
	                       HttpSession session,
	                       @Valid @ModelAttribute(value = "searchCommand") TiersCriteriaView criteria,
	                       BindingResult bindingResult) {

		if (bindingResult.hasErrors()) {
			return showList(model, criteria);
		}

		session.setAttribute(CRITERIA_SESSION_NAME, criteria);
		return "redirect:list.do";
	}

	@RequestMapping(value = "/recap.do", method = RequestMethod.GET)
	public String showRecapitulatif(Model model, @RequestParam(value = "numeroCple") long idMenage) {
		controllerUtils.checkAccesDossierEnLecture(idMenage);
		return showRecapitulatif(model, new SeparationRecapView(idMenage));
	}

	private String showRecapitulatif(Model model, SeparationRecapView view) {
		model.addAttribute("idMenage", view.getIdMenage());
		model.addAttribute("separationCommand", view);
		return "tiers/edition/separation/recap";
	}

	@RequestMapping(value = "/commit.do", method = RequestMethod.POST)
	public String doAnnulerMenage(Model model,
	                              @Valid @ModelAttribute(value = "separationCommand") SeparationRecapView view,
	                              BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return showRecapitulatif(model, view);
		}

		controllerUtils.checkAccesDossierEnEcriture(view.getIdMenage());
		controllerUtils.checkTraitementContribuableAvecDecisionAci(view.getIdMenage());
		try {
			separationRecapManager.separeCouple(view.getIdMenage(), view.getDateSeparation(), view.getEtatCivil(), view.getRemarque());
		}
		catch (MetierServiceException e) {
			throw new ActionException(e.getMessage(), e);
		}
		return "redirect:/tiers/visu.do?id=" + view.getIdMenage();
	}
}
