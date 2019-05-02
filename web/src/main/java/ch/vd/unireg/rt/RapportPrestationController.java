package ch.vd.unireg.rt;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.common.ControllerUtils;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.common.URLHelper;
import ch.vd.unireg.common.pagination.WebParamPagination;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.indexer.TooManyResultsIndexerException;
import ch.vd.unireg.rapport.SensRapportEntreTiers;
import ch.vd.unireg.rapport.manager.RapportEditManager;
import ch.vd.unireg.rt.manager.RapportPrestationEditManager;
import ch.vd.unireg.rt.view.DebiteurListView;
import ch.vd.unireg.rt.view.RapportPrestationView;
import ch.vd.unireg.rt.view.SourcierListView;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityCheck;
import ch.vd.unireg.tiers.TiersIndexedDataView;
import ch.vd.unireg.tiers.TiersMapHelper;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.view.RapportsPrestationView;
import ch.vd.unireg.tiers.view.TiersEditView;
import ch.vd.unireg.utils.RegDateEditor;

/**
 * Contrôleur qui gère les rapports de prestations entre débiteurs et sourciers.
 */
@Controller
public class RapportPrestationController {

	private static final Logger LOGGER = LoggerFactory.getLogger(RapportPrestationController.class);

	public static final String DROIT_CONSULTATION_RT = "vous ne possédez pas les droits de consultation des rapports de prestations imposables";
	public static final String DROIT_MODIFICATION_RT = "vous ne possédez pas les droits de modification des rapports de prestations imposables";

	private static final String TABLE_NAME = "rapportPrestation";
	private static final int PAGE_SIZE = 10;

	private TiersService tiersService;
	private TiersMapHelper tiersMapHelper;
	private RapportEditManager rapportEditManager;
	private RapportPrestationEditManager rapportPrestationEditManager;
	private ControllerUtils controllerUtils;

	private Validator rapportEditValidator;
	private Validator tiersCriteriaValidator;

	/**
	 * Affiche l'écran d'édition des rapports de prestation d'un débiteur
	 *
	 * @param id le numéro de tiers du débiteur
	 */
	@SecurityCheck(rolesToCheck = {Role.RT, Role.CREATE_MODIF_DPI}, accessDeniedMessage = DROIT_MODIFICATION_RT)
	@RequestMapping(value = "/rapports-prestation/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String edit(@RequestParam long id, Model model, HttpServletRequest request) throws AdresseException {

		controllerUtils.checkAccesDossierEnEcriture(id);

		final WebParamPagination pagination = new WebParamPagination(request, TABLE_NAME, PAGE_SIZE);
		final TiersEditView view = rapportEditManager.getRapportsPrestationView(id, pagination, true);
		view.getContribuablesAssocies().forEach(rapport -> rapport.setSensRapportEntreTiers(SensRapportEntreTiers.OBJET));
		final int rapportCount = rapportPrestationEditManager.countRapportsPrestationImposable(id, true);
		model.addAttribute("resultSize", rapportCount);
		model.addAttribute("command", view);

		return "tiers/edition/rt/edit";
	}

	@InitBinder({"debiteurCriteriaView", "sourcierCriteriaView"})
	public void initTiersCriteriaBinder(WebDataBinder binder) {
		binder.setValidator(tiersCriteriaValidator);
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
	}

	/**
	 * Affiche l'écran de recherche d'un débiteur à lier par un rapport de prestation à un contribuable donné.
	 */
	@SecurityCheck(rolesToCheck = {Role.RT, Role.CREATE_MODIF_DPI}, accessDeniedMessage = DROIT_MODIFICATION_RT)
	@RequestMapping(value = "/rapports-prestation/search-debiteur.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String searchDebiteur(@Valid @ModelAttribute("debiteurCriteriaView") final DebiteurListView view, BindingResult binding, Model model) {

		final long idSourcier = view.getNumeroSourcier();

		if (!rapportPrestationEditManager.isExistingTiers(idSourcier)) {
			throw new TiersNotFoundException(idSourcier);
		}

		// checks de sécurité
		controllerUtils.checkAccesDossierEnEcriture(idSourcier);

		model.addAttribute("typesRechercheNom", tiersMapHelper.getMapTypeRechercheNom());
		model.addAttribute("formesJuridiquesEnum", tiersMapHelper.getMapFormesJuridiquesEntreprise());
		model.addAttribute("categoriesEntreprisesEnum", tiersMapHelper.getMapCategoriesEntreprise());

		if (binding.hasErrors() || view.isEmpty()) {
			return "tiers/edition/rt/debiteur/list";
		}

		// on effectue la recherche
		try {
			final List<TiersIndexedDataView> list = tiersService.search(view.asCore()).stream()
					.map(TiersIndexedDataView::new)
					.collect(Collectors.toList());
			model.addAttribute("list", list);
		}
		catch (TooManyResultsIndexerException ee) {
			if (ee.getNbResults() > 0) {
				binding.reject("error.preciser.recherche.trouves", new Object[]{String.valueOf(ee.getNbResults())}, null);
			}
			else {
				binding.reject("error.preciser.recherche");
			}
		}
		catch (IndexerException e) {
			LOGGER.error("Exception dans l'indexer: " + e.getMessage(), e);
			binding.reject("error.recherche");
		}

		return "tiers/edition/rt/debiteur/list";
	}

	/**
	 * Affiche l'écran de recherche d'un sourcier à lier par un rapport de prestation à un débiteur donné.
	 */
	@SecurityCheck(rolesToCheck = {Role.RT, Role.CREATE_MODIF_DPI}, accessDeniedMessage = DROIT_MODIFICATION_RT)
	@RequestMapping(value = "/rapports-prestation/search-sourcier.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String searchSourcier(@Valid @ModelAttribute("sourcierCriteriaView") final SourcierListView view, BindingResult binding, Model model) {

		final long idDebiteur = view.getNumeroDebiteur();

		if (!rapportPrestationEditManager.isExistingTiers(idDebiteur)) {
			throw new TiersNotFoundException(idDebiteur);
		}

		// checks de sécurité
		controllerUtils.checkAccesDossierEnEcriture(idDebiteur);

		model.addAttribute("typesRechercheNom", tiersMapHelper.getMapTypeRechercheNom());
		model.addAttribute("formesJuridiquesEnum", tiersMapHelper.getMapFormesJuridiquesEntreprise());
		model.addAttribute("categoriesEntreprisesEnum", tiersMapHelper.getMapCategoriesEntreprise());

		if (binding.hasErrors() || view.isEmpty()) {
			return "tiers/edition/rt/sourcier/list";
		}

		// on effectue la recherche
		try {
			final List<TiersIndexedDataView> list = tiersService.search(view.asCore()).stream()
					.map(TiersIndexedDataView::new)
					.collect(Collectors.toList());
			model.addAttribute("list", list);
		}
		catch (TooManyResultsIndexerException ee) {
			if (ee.getNbResults() > 0) {
				binding.reject("error.preciser.recherche.trouves", new Object[]{String.valueOf(ee.getNbResults())}, null);
			}
			else {
				binding.reject("error.preciser.recherche");
			}
		}
		catch (IndexerException e) {
			LOGGER.error("Exception dans l'indexer: " + e.getMessage(), e);
			binding.reject("error.recherche");
		}

		return "tiers/edition/rt/sourcier/list";
	}

	/**
	 * Affiche l'écran de récapitulation avant ajout d'un rapport de prestations imposables.
	 *
	 * @param sourcierId l'id du sourcier
	 * @param debiteurId l'id du débiteur
	 */
	@SecurityCheck(rolesToCheck = {Role.RT, Role.CREATE_MODIF_DPI}, accessDeniedMessage = DROIT_MODIFICATION_RT)
	@RequestMapping(value = "/rapports-prestation/add.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String addRapport(@RequestParam(value = "numeroSrc") long sourcierId, @RequestParam(value = "numeroDpi") long debiteurId, Model model) {

		controllerUtils.checkAccesDossierEnEcriture(sourcierId);
		controllerUtils.checkAccesDossierEnEcriture(debiteurId);

		final RapportPrestationView view = rapportPrestationEditManager.get(sourcierId, debiteurId, "ne pas utiliser");
		model.addAttribute("rapportAddView", view);

		return "tiers/edition/rt/contrat/edit";
	}

	@InitBinder("rapportAddView")
	public void initBinder(WebDataBinder binder) {
		binder.setValidator(rapportEditValidator);
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
	}

	/**
	 * Enregistre un nouvel rapport de prestations imposables dans la DB
	 */
	@SecurityCheck(rolesToCheck = {Role.RT, Role.CREATE_MODIF_DPI}, accessDeniedMessage = DROIT_MODIFICATION_RT)
	@RequestMapping(value = "/rapports-prestation/add.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String addRapport(@Valid @ModelAttribute(value = "rapportAddView") RapportPrestationView view, BindingResult binding, Model model) {

		if (binding.hasErrors()) {
			model.addAttribute("rapportAddView", view);
			return "tiers/edition/rt/contrat/edit";
		}

		final Long idDpi = view.getDebiteur().getNumero();
		controllerUtils.checkAccesDossierEnEcriture(idDpi);
		controllerUtils.checkAccesDossierEnEcriture(view.getSourcier().getNumero());

		// on ajoute le rapport
		rapportPrestationEditManager.save(view);

		// on retourne à l'écran précédent
		return URLHelper
				.navigateBackTo("/rapports-prestation/edit.do")     // écran d'édition des rapports d'un débiteur
				.or("/rapport/list.do")                             // écran d'édition des rapports d'un contribuable
				.defaultTo("/rapports-prestation/edit.do", "id=" + idDpi);
	}

	/**
	 * Annule un rapport de prestations imposables.
	 *
	 * @param rapportId l'id du rapport à annuler
	 */
	@SecurityCheck(rolesToCheck = {Role.RT, Role.CREATE_MODIF_DPI}, accessDeniedMessage = DROIT_MODIFICATION_RT)
	@RequestMapping(value = "/rapports-prestation/cancel.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String annulerRapport(@RequestParam long rapportId) {

		final long idDpi = rapportEditManager.getDebiteurId(rapportId);
		controllerUtils.checkAccesDossierEnEcriture(idDpi);

		// on annule le rapport
		rapportEditManager.annulerRapportPrestation(rapportId);

		// on retourne à l'écran précédent
		return URLHelper
				.navigateBackTo("/rapports-prestation/edit.do") // écran d'édition des rapports d'un débiteur
				.or("/rapport/list.do")                         // écran d'édition des rapports d'un contribuable
				.or("/rapports-prestation/full-list.do")        // écran d'affichage de tous les rapports d'un débiteur
				.defaultTo("/rapports-prestation/edit.do", "id=" + idDpi);
	}

	/**
	 * Affiche la liste complète (non-paginée) des rapports de travail du débiteur spécifié.
	 *
	 * @param idDpi le numéro de tiers du débiteur
	 */
	@SecurityCheck(rolesToCheck = Role.VISU_ALL, accessDeniedMessage = DROIT_CONSULTATION_RT)
	@RequestMapping(value = "/rapports-prestation/full-list.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String fullList(@RequestParam long idDpi, Model model) {

		controllerUtils.checkAccesDossierEnLecture(idDpi);

		final RapportsPrestationView view = new RapportsPrestationView();
		rapportPrestationEditManager.fillRapportsPrestationView(idDpi, view);
		model.addAttribute("command", view);

		return "tiers/visualisation/rt/list";
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setRapportEditManager(RapportEditManager rapportEditManager) {
		this.rapportEditManager = rapportEditManager;
	}

	public void setRapportPrestationEditManager(RapportPrestationEditManager rapportPrestationEditManager) {
		this.rapportPrestationEditManager = rapportPrestationEditManager;
	}

	public void setRapportEditValidator(Validator rapportEditValidator) {
		this.rapportEditValidator = rapportEditValidator;
	}

	public void setTiersCriteriaValidator(Validator tiersCriteriaValidator) {
		this.tiersCriteriaValidator = tiersCriteriaValidator;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}
}

