package ch.vd.unireg.complements;

import javax.validation.Valid;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.ControllerUtils;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.coordfin.CoordonneesFinancieresService;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.tiers.CoordonneesFinancieres;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.manager.AutorisationManager;
import ch.vd.unireg.tiers.manager.Autorisations;
import ch.vd.unireg.utils.RegDateEditor;

@Controller
@RequestMapping(value = "/complements")
public class CoordonneesFinancieresController {

	private HibernateTemplate hibernateTemplate;
	private AutorisationManager autorisationManager;
	private ControllerUtils controllerUtils;
	private Validator addValidator;
	private Validator editValidator;
	private CoordonneesFinancieresService coordonneesFinancieresService;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setAutorisationManager(AutorisationManager autorisationManager) {
		this.autorisationManager = autorisationManager;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setAddValidator(Validator addValidator) {
		this.addValidator = addValidator;
	}

	public void setEditValidator(Validator editValidator) {
		this.editValidator = editValidator;
	}

	public void setCoordonneesFinancieresService(CoordonneesFinancieresService coordonneesFinancieresService) {
		this.coordonneesFinancieresService = coordonneesFinancieresService;
	}

	@InitBinder(value = "addCoords")
	protected void initAddBinder(WebDataBinder binder) {
		binder.setValidator(addValidator);
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
	}

	@InitBinder(value = "editCoords")
	protected void initEditBinder(WebDataBinder binder) {
		binder.setValidator(editValidator);
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
	}

	/**
	 * Affiche la liste des coordonnées financières d'un tiers pour l'édition.
	 */
	@RequestMapping(value = "/coordfinancieres/list.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String list(@RequestParam(value = "tiersId") Long tiersId, Model model) throws Exception {

		final Tiers tiers = hibernateTemplate.get(Tiers.class, tiersId);
		if (tiers == null) {
			throw new TiersNotFoundException(tiersId);
		}

		final Autorisations autorisations = autorisationManager.getAutorisations(tiers, AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID());
		if (!autorisations.isComplements()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition des compléments d'un tiers");
		}

		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		final List<CoordonneesFinancieresEditView> list = tiers.getCoordonneesFinancieres().stream()
				.sorted(new AnnulableHelper.AnnulableDateRangeComparator<>(true))
				.map(CoordonneesFinancieresEditView::new)
				.collect(Collectors.toList());

		model.addAttribute("tiersId", tiersId);
		model.addAttribute("tiersNumeroFormatter", FormatNumeroHelper.numeroCTBToDisplay(tiersId));
		model.addAttribute("coordonneesFinancieres", list);

		return "complements/coordfinancieres/list";
	}

	/**
	 * Affiche l'écran d'ajout de coordonnées financières
	 */
	@RequestMapping(value = "/coordfinancieres/add.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String add(@RequestParam(value = "tiersId") Long tiersId, Model model) throws Exception {

		final Tiers tiers = hibernateTemplate.get(Tiers.class, tiersId);
		if (tiers == null) {
			throw new TiersNotFoundException(tiersId);
		}

		final Autorisations autorisations = autorisationManager.getAutorisations(tiers, AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID());
		if (!autorisations.isComplements()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition des compléments d'un tiers");
		}

		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		final CoordonneesFinancieresEditView view = new CoordonneesFinancieresEditView();
		final Date now = DateHelper.getCurrentDate();
		view.setDateDebut(RegDateHelper.get(now));
		model.addAttribute("addCoords", view);
		model.addAttribute("tiersId", tiersId);
		model.addAttribute("tiersNumeroFormatter", FormatNumeroHelper.numeroCTBToDisplay(tiersId));

		return "complements/coordfinancieres/add";
	}

	/**
	 * Ajoute de nouvelles coordonnées financières
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/coordfinancieres/add.do", method = RequestMethod.POST)
	public String add(@RequestParam(value = "tiersId") Long tiersId,
	                  @Valid @ModelAttribute("addCoords") final CoordonneesFinancieresEditView view, BindingResult result,
	                  Model model) throws Exception {

		final Tiers tiers = hibernateTemplate.get(Tiers.class, tiersId);
		if (tiers == null) {
			throw new TiersNotFoundException(tiersId);
		}

		final Autorisations autorisations = autorisationManager.getAutorisations(tiers, AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID());
		if (!autorisations.isComplements() || !autorisations.isComplementsCoordonneesFinancieres()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition des coordonnées financières d'un tiers");
		}

		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		if (result.hasErrors()) {
			model.addAttribute("tiersId", tiersId);
			model.addAttribute("tiersNumeroFormatter", FormatNumeroHelper.numeroCTBToDisplay(tiersId));
			return "complements/coordfinancieres/add";
		}

		// on ajoute les données.
		coordonneesFinancieresService.addCoordonneesFinancieres(tiers,
		                                                        view.getDateDebut(),
		                                                        view.getDateFin(),
		                                                        view.getTitulaireCompteBancaire(),
		                                                        view.getIban(),
		                                                        view.getAdresseBicSwift());

		return "redirect:/complements/coordfinancieres/list.do?tiersId=" + tiersId;
	}

	/**
	 * Affiche l'écran d'édition de coordonnées financières existantes
	 */
	@RequestMapping(value = "/coordfinancieres/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String edit(@RequestParam(value = "id") Long id, Model model) throws Exception {

		final CoordonneesFinancieres coords = hibernateTemplate.get(CoordonneesFinancieres.class, id);
		if (coords == null) {
			throw new ObjectNotFoundException("Les coordonnées financières avec l'id=" + id + " n'existent pas.");
		}

		final Autorisations autorisations = autorisationManager.getAutorisations(coords.getTiers(), AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID());
		if (!autorisations.isComplements()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition des compléments d'un tiers");
		}

		final Long tiersId = coords.getTiers().getNumero();
		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		final CoordonneesFinancieresEditView view = new CoordonneesFinancieresEditView(coords);
		model.addAttribute("editCoords", view);
		model.addAttribute("tiersId", tiersId);
		model.addAttribute("tiersNumeroFormatter", FormatNumeroHelper.numeroCTBToDisplay(tiersId));

		return "complements/coordfinancieres/edit";
	}

	/**
	 * Enregistre les modifications apportées à des coordonnées financières existantes
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/coordfinancieres/edit.do", method = RequestMethod.POST)
	public String edit(@Valid @ModelAttribute("editCoords") final CoordonneesFinancieresEditView view, BindingResult result, Model model) throws Exception {

		final Long id = view.getId();
		final CoordonneesFinancieres coords = hibernateTemplate.get(CoordonneesFinancieres.class, id);
		if (coords == null) {
			throw new ObjectNotFoundException("Les coordonnées avec l'id=[" + id + "] n'existent pas");
		}
		final Tiers tiers = coords.getTiers();
		final Long tiersId = tiers.getNumero();

		final Autorisations autorisations = autorisationManager.getAutorisations(tiers, AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID());
		if (!autorisations.isComplements() || !autorisations.isComplementsCoordonneesFinancieres()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition des coordonnées financières d'un tiers");
		}

		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		if (result.hasErrors()) {
			model.addAttribute("tiersId", tiersId);
			return "complements/coordfinancieres/edit";
		}

		// on met-à-jour les données.
		coordonneesFinancieresService.updateCoordonneesFinancieres(id,
		                                                           view.getDateFin(),
		                                                           view.getTitulaireCompteBancaire(),
		                                                           view.getIban(),
		                                                           view.getAdresseBicSwift());

		return "redirect:/complements/coordfinancieres/list.do?tiersId=" + tiersId;
	}

	/**
	 * Annule des coordonnées financières existantes
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/coordfinancieres/cancel.do", method = RequestMethod.POST)
	public String cancel(@RequestParam(value = "id") Long id) throws Exception {

		final CoordonneesFinancieres coords = hibernateTemplate.get(CoordonneesFinancieres.class, id);
		if (coords == null) {
			throw new ObjectNotFoundException("Les coordonnées avec l'id=[" + id + "] n'existent pas");
		}
		final Tiers tiers = coords.getTiers();
		final Long tiersId = tiers.getNumero();

		final Autorisations autorisations = autorisationManager.getAutorisations(tiers, AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID());
		if (!autorisations.isComplements() || !autorisations.isComplementsCoordonneesFinancieres()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec d'édition des coordonnées financières d'un tiers");
		}

		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		// on annule les données.
		coordonneesFinancieresService.cancelCoordonneesFinancieres(id);

		return "redirect:/complements/coordfinancieres/list.do?tiersId=" + tiersId;
	}
}
