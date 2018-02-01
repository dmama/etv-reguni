package ch.vd.unireg.activation;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
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
import ch.vd.unireg.activation.manager.TiersAnnulationRecapManager;
import ch.vd.unireg.activation.manager.TiersReactivationRecapManager;
import ch.vd.unireg.activation.validator.TiersReactivationRecapValidator;
import ch.vd.unireg.activation.view.TiersAnnulationRecapView;
import ch.vd.unireg.activation.view.TiersReactivationRecapView;
import ch.vd.unireg.common.ActionException;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.indexer.EmptySearchCriteriaException;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.indexer.TooManyResultsIndexerException;
import ch.vd.unireg.tiers.NatureTiers;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.TiersIndexedDataView;
import ch.vd.unireg.tiers.TiersMapHelper;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.TypeTiers;
import ch.vd.unireg.tiers.validator.TiersCriteriaValidator;
import ch.vd.unireg.tiers.view.TiersCriteriaView;
import ch.vd.unireg.utils.RegDateEditor;
import ch.vd.unireg.utils.WebContextUtils;

@Controller
@RequestMapping(value = "/activation")
public class TiersActivationController implements MessageSourceAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(TiersActivationController.class);

	public static final String ACTIVATION_CRITERIA_NAME = "ActivationCriteria";
	public static final String ACTIVATION_LIST_ATTRIBUTE_NAME = "list";

	/**
	 * Mode de fonctionnement
	 */
	public enum ActivationMode {
		/**
		 * Désactivation/annulation de tiers
		 */
		DESACTIVATION,

		/**
		 * Réactivation de tiers
		 */
		REACTIVATION
	}

	/**
	 * Type de population considérée
	 */
	public enum TypePopulation {
		/**
		 * PP et assimilés (= historiquement, tous tiers sauf PM)
		 */
		PP,

		/**
		 * Entreprises, établissements...
		 */
		PM
	}

	private static final Map<ActivationMode, Predicate<? super TiersIndexedDataView>> RESULT_FILTERS = buildResultFilters();

	private TiersService tiersService;
	private TiersMapHelper tiersMapHelper;
	private MessageSource messageSource;
	private TiersAnnulationRecapManager tiersAnnulationRecapManager;
	private Validator tiersAnnulationRecapValidator;
	private TiersReactivationRecapManager tiersReactivationRecapManager;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setTiersAnnulationRecapManager(TiersAnnulationRecapManager tiersAnnulationRecapManager) {
		this.tiersAnnulationRecapManager = tiersAnnulationRecapManager;
	}

	public void setTiersAnnulationRecapValidator(Validator tiersAnnulationRecapValidator) {
		this.tiersAnnulationRecapValidator = tiersAnnulationRecapValidator;
	}

	public void setTiersReactivationRecapManager(TiersReactivationRecapManager tiersReactivationRecapManager) {
		this.tiersReactivationRecapManager = tiersReactivationRecapManager;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	private static Map<ActivationMode, Predicate<? super TiersIndexedDataView>> buildResultFilters() {
		final Map<ActivationMode, Predicate<? super TiersIndexedDataView>> map = new EnumMap<>(ActivationMode.class);
		map.put(ActivationMode.DESACTIVATION, AnnulableHelper::nonAnnule);
		map.put(ActivationMode.REACTIVATION, Annulable::isAnnule);
		return map;
	}

	private static String buildCriteriaSessionName(ActivationMode mode, TypePopulation typePopulation) {
		return String.format("%s-%s-%s", ACTIVATION_CRITERIA_NAME, mode, typePopulation);
	}

	private static TiersCriteriaView getCriteriaFromSessionOrNew(HttpSession session, ActivationMode mode, TypePopulation typePopulation) {
		final String name = buildCriteriaSessionName(mode, typePopulation);
		return Optional.of(session)
				.map(s -> s.getAttribute(name))
				.map(TiersCriteriaView.class::cast)
				.orElseGet(TiersCriteriaView::new);
	}

	private static void fillMandatorySearchCriteria(TiersCriteriaView view, ActivationMode mode, TypePopulation typePopulation) {
		final Set<TiersCriteria.TypeTiers> types = EnumSet.noneOf(TiersCriteria.TypeTiers.class);
		switch (typePopulation) {
		case PM:
			types.add(TiersCriteria.TypeTiers.ENTREPRISE);
			types.add(TiersCriteria.TypeTiers.ETABLISSEMENT_SECONDAIRE);
			break;

		case PP:
			types.add(TiersCriteria.TypeTiers.DEBITEUR_PRESTATION_IMPOSABLE);
			types.add(TiersCriteria.TypeTiers.AUTRE_COMMUNAUTE);
			types.add(TiersCriteria.TypeTiers.COLLECTIVITE_ADMINISTRATIVE);
			types.add(TiersCriteria.TypeTiers.CONTRIBUABLE_PP);
			break;

		default:
			throw new IllegalArgumentException("Tyde de population non-supporté : " + typePopulation);
		}
		view.setTypesTiersImperatifs(types);

		final boolean tiersAnnules;
		switch (mode) {
		case DESACTIVATION:
			tiersAnnules = false;
			break;
		case REACTIVATION:
			tiersAnnules = true;
			break;
		default:
			throw new IllegalArgumentException("Mode d'activation non-supporté : " + mode);
		}
		view.setInclureTiersAnnules(tiersAnnules);
		view.setTiersAnnulesSeulement(tiersAnnules);
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

	@InitBinder(value = "deactivationCommand")
	public void initDeactivationCommandBinder(WebDataBinder binder) {
		binder.setValidator(tiersAnnulationRecapValidator);
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(false, false, false, RegDateHelper.StringFormat.DISPLAY));
	}

	@InitBinder(value = "reactivationCommand")
	public void initReactivationCommandBinder(WebDataBinder binder) {
		binder.setValidator(new TiersReactivationRecapValidator());
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(false, false, false, RegDateHelper.StringFormat.DISPLAY));
	}

	@RequestMapping(value = "/reset-search.do", method = RequestMethod.GET)
	public String resetSearchCriteria(HttpSession session,
	                                  @RequestParam(value = "mode") ActivationMode mode,
	                                  @RequestParam(value = "population") TypePopulation typePopulation) {
		final String name = buildCriteriaSessionName(mode, typePopulation);
		session.removeAttribute(name);
		return "redirect:list.do?mode=" + mode + "&population=" + typePopulation;
	}

	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	public String showList(Model model,
	                       HttpSession session,
	                       @RequestParam(value = "mode") ActivationMode mode,
	                       @RequestParam(value = "population") TypePopulation typePopulation) {

		final TiersCriteriaView view = getCriteriaFromSessionOrNew(session, mode, typePopulation);
		fillMandatorySearchCriteria(view, mode, typePopulation);

		String searchError = null;
		try {
			final Predicate<? super TiersIndexedDataView> filter = RESULT_FILTERS.getOrDefault(mode, x -> true);
			final List<TiersIndexedDataView> results = searchTiers(view).stream()
					.filter(filter)
					.collect(Collectors.toList());
			model.addAttribute(ACTIVATION_LIST_ATTRIBUTE_NAME, results);
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
		model.addAttribute("formesJuridiquesEnum", tiersMapHelper.getMapFormesJuridiquesEntreprise());
		model.addAttribute("categoriesEntreprisesEnum", tiersMapHelper.getMapCategoriesEntreprise());
		model.addAttribute("searchCommand", criteria);
		return "activation/list";
	}

	@RequestMapping(value = "/list.do", method = RequestMethod.POST)
	public String doSearch(Model model,
	                       HttpSession session,
	                       @Valid @ModelAttribute(value = "searchCommand") TiersCriteriaView criteria,
	                       BindingResult bindingResult,
	                       @RequestParam(value = "mode") ActivationMode mode,
	                       @RequestParam(value = "population") TypePopulation typePopulation) {

		if (bindingResult.hasErrors()) {
			return showList(model, criteria);
		}

		final String name = buildCriteriaSessionName(mode, typePopulation);
		session.setAttribute(name, criteria);

		return "redirect:list.do?mode=" + mode + "&population=" + typePopulation;
	}

	@RequestMapping(value = "/deactivate.do", method = RequestMethod.GET)
	public String deactivate(Model model, @RequestParam(value = "numero") long idTiers) {
		final TiersAnnulationRecapView view = tiersAnnulationRecapManager.get(idTiers);
		final TypePopulation typePopulation = getTypePopulation(view.getTypeTiers());
		return showDeactivate(model, typePopulation, view);
	}

	private String showDeactivate(Model model, TypePopulation typePopulation, TiersAnnulationRecapView view) {
		model.addAttribute("deactivationCommand", view);
		model.addAttribute("population", typePopulation);
		return "activation/annulation/recap";
	}

	@RequestMapping(value = "/deactivate.do", method = RequestMethod.POST)
	public String doDeactivate(Model model,
	                           @Valid @ModelAttribute(value = "deactivationCommand") TiersAnnulationRecapView view,
	                           BindingResult bindingResult,
	                           @RequestParam(value = "population") TypePopulation typePopulation) {

		if (bindingResult.hasErrors()) {
			return showDeactivate(model, typePopulation, view);
		}
		tiersAnnulationRecapManager.save(view);
		return "redirect:/tiers/visu.do?id=" + view.getNumeroTiers();
	}

	@RequestMapping(value = "/reactivate.do", method = RequestMethod.GET)
	public String reactivate(Model model, @RequestParam(value = "numero") long idTiers) {
		final TiersReactivationRecapView view = tiersReactivationRecapManager.get(idTiers);
		final TypePopulation typePopulation = getTypePopulation(view.getTiers().getNatureTiers());
		return showReactivate(model, typePopulation, view);
	}

	private String showReactivate(Model model, TypePopulation typePopulation, TiersReactivationRecapView view) {
		model.addAttribute("reactivationCommand", view);
		model.addAttribute("population", typePopulation);
		return "activation/reactivation/recap";
	}

	@RequestMapping(value = "/reactivate.do", method = RequestMethod.POST)
	public String doReactivate(Model model,
	                           @Valid @ModelAttribute(value = "reactivationCommand") TiersReactivationRecapView view,
	                           BindingResult bindingResult,
	                           @RequestParam(value = "population") TypePopulation typePopulation) {

		if (bindingResult.hasErrors()) {
			return showReactivate(model, typePopulation, view);
		}
		try {
			tiersReactivationRecapManager.save(view);
			return "redirect:/tiers/visu.do?id=" + view.getTiers().getNumero();
		}
		catch (ActivationServiceException e) {
			LOGGER.error("L'opération de réactication de tiers a échoué", e);
			throw new ActionException(e.getMessage());
		}
	}

	private static TypePopulation getTypePopulation(NatureTiers natureTiers) {
		if (natureTiers == null) {
			return null;
		}

		switch (natureTiers) {
		case AutreCommunaute:
		case CollectiviteAdministrative:
		case DebiteurPrestationImposable:
		case Habitant:
		case MenageCommun:
		case NonHabitant:
			return TypePopulation.PP;
		case Entreprise:
		case Etablissement:
			return TypePopulation.PM;
		default:
			throw new IllegalArgumentException("Nature de tiers non-supportée : " + natureTiers);
		}
	}

	private static TypePopulation getTypePopulation(TypeTiers typeTiers) {
		if (typeTiers == null) {
			return null;
		}

		switch (typeTiers) {
		case AUTRE_COMMUNAUTE:
		case COLLECTIVITE_ADMINISTRATIVE:
		case DEBITEUR_PRESTATION_IMPOSABLE:
		case MENAGE_COMMUN:
		case PERSONNE_PHYSIQUE:
			return TypePopulation.PP;
		case ENTREPRISE:
		case ETABLISSEMENT:
			return TypePopulation.PM;
		default:
			throw new IllegalArgumentException("Type de tiers non-supporté : " + typeTiers);
		}
	}
}
