package ch.vd.unireg.rt;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

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
import ch.vd.unireg.common.URLHelper;
import ch.vd.unireg.common.pagination.WebParamPagination;
import ch.vd.unireg.rapport.manager.RapportEditManager;
import ch.vd.unireg.rt.manager.RapportPrestationEditManager;
import ch.vd.unireg.rt.view.RapportPrestationView;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityCheck;
import ch.vd.unireg.tiers.manager.TiersEditManager;
import ch.vd.unireg.tiers.manager.TiersVisuManager;
import ch.vd.unireg.tiers.view.RapportsPrestationView;
import ch.vd.unireg.tiers.view.TiersEditView;
import ch.vd.unireg.utils.RegDateEditor;

/**
 * Contrôleur qui gère les rapports de prestations entre débiteurs et sourciers.
 */
@Controller
public class RapportPrestationController {

	public static final String DROIT_CONSULTATION_RT = "vous ne possédez pas le droit IfoSec de consultation des rapports de prestations imposables";
	public static final String DROIT_MODIFICATION_RT = "vous ne possédez pas le droit IfoSec de modification des rapports de prestations imposables";

	private static final String TABLE_NAME = "rapportPrestation";
	private static final int PAGE_SIZE = 10;

	private TiersVisuManager tiersVisuManager;
	private TiersEditManager tiersEditManager;
	private RapportEditManager rapportEditManager;
	private RapportPrestationEditManager rapportPrestationEditManager;
	private ControllerUtils controllerUtils;
	private Validator rapportEditValidator;

	/**
	 * Affiche l'écran d'édition des rapports de prestation d'un débiteur
	 *
	 * @param id le numéro de tiers du débiteur
	 */
	@SecurityCheck(rolesToCheck = Role.RT, accessDeniedMessage = DROIT_MODIFICATION_RT)
	@RequestMapping(value = "/rapports-prestation/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String edit(@RequestParam long id, Model model, HttpServletRequest request) throws AdresseException {

		controllerUtils.checkAccesDossierEnEcriture(id);

		final WebParamPagination pagination = new WebParamPagination(request, TABLE_NAME, PAGE_SIZE);
		final TiersEditView view = rapportEditManager.getRapportsPrestationView(id, pagination, true);
		final int rapportCount = tiersEditManager.countRapportsPrestationImposable(id, true);
		model.addAttribute("resultSize", rapportCount);
		model.addAttribute("command", view);

		return "tiers/edition/rt/edit";
	}

	/**
	 * Affiche l'écran de récapitulation avant ajout d'un rapport de prestations imposables.
	 *
	 * @param sourcierId l'id du sourcier
	 * @param debiteurId l'id du débiteur
	 */
	@SecurityCheck(rolesToCheck = Role.RT, accessDeniedMessage = DROIT_MODIFICATION_RT)
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
	@SecurityCheck(rolesToCheck = Role.RT, accessDeniedMessage = DROIT_MODIFICATION_RT)
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
	@SecurityCheck(rolesToCheck = Role.RT, accessDeniedMessage = DROIT_MODIFICATION_RT)
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
		tiersVisuManager.fillRapportsPrestationView(idDpi, view);
		model.addAttribute("command", view);

		return "tiers/visualisation/rt/list";
	}

	public void setTiersVisuManager(TiersVisuManager tiersVisuManager) {
		this.tiersVisuManager = tiersVisuManager;
	}

	public void setTiersEditManager(TiersEditManager tiersEditManager) {
		this.tiersEditManager = tiersEditManager;
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

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}
}

