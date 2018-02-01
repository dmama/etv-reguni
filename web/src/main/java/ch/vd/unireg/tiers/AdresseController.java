package ch.vd.unireg.tiers;

import javax.validation.Valid;

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
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.common.ControllerUtils;
import ch.vd.unireg.tiers.manager.AdresseManager;
import ch.vd.unireg.tiers.view.AdresseView;
import ch.vd.unireg.tiers.view.CloseAdresseView;
import ch.vd.unireg.utils.RegDateEditor;

@Controller
@RequestMapping(value = "/adresses")
public class AdresseController {

	private static final String ID_ADRESSE_PARAMETER_NAME = "idAdresse";

	private AdresseManager adresseManager;
	private ControllerUtils controllerUtils;
	private TiersMapHelper tiersMapHelper;

	private Validator closeAdresseValidator;
	private Validator editAdresseValidator;

	public void setAdresseManager(AdresseManager adresseManager) {
		this.adresseManager = adresseManager;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setCloseAdresseValidator(Validator closeAdresseValidator) {
		this.closeAdresseValidator = closeAdresseValidator;
	}

	public void setEditAdresseValidator(Validator editAdresseValidator) {
		this.editAdresseValidator = editAdresseValidator;
	}

	@InitBinder(value = "closeCommand")
	public void initBinderCloseAdresse(WebDataBinder binder) {
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
		binder.setValidator(closeAdresseValidator);
	}

	@InitBinder(value = "editCommand")
	public void initBinderAddAddress(WebDataBinder binder) {
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
		binder.setValidator(editAdresseValidator);
	}

	@RequestMapping(value = "edit.do", method = RequestMethod.GET)
	public String showEditMode(Model model, @RequestParam(value = "id") long idTiers) throws AdresseException {
		controllerUtils.checkAccesDossierEnEcriture(idTiers);
		model.addAttribute("command", adresseManager.getView(idTiers));
		return "tiers/edition/adresse/edit";
	}

	@RequestMapping(value = "cancel.do", method = RequestMethod.POST)
	public String annulerAdresse(@RequestParam(value = "id") long idAdresse, @RequestParam(value = "idTiers") long idTiers) {
		controllerUtils.checkAccesDossierEnEcriture(idTiers);
		adresseManager.annulerAdresse(idAdresse);
		return "redirect:edit.do?id=" + idTiers;
	}

	@RequestMapping(value = "adresse-close.do", method = RequestMethod.GET)
	public String showCloseAddress(Model model, @RequestParam(value = ID_ADRESSE_PARAMETER_NAME) long idAdresse) {
		final AdresseView adresseAFermerView = adresseManager.getAdresseView(idAdresse);
		if (adresseAFermerView != null) {
			controllerUtils.checkAccesDossierEnEcriture(adresseAFermerView.getNumCTB());
		}
		return showCloseAddress(model, idAdresse, new CloseAdresseView());
	}

	private String showCloseAddress(Model model, long idAdresse, CloseAdresseView closeView) {
		final AdresseView view = adresseManager.getAdresseView(idAdresse);
		model.addAttribute("view", view);
		model.addAttribute("closeCommand", closeView);
		return "tiers/edition/adresse/adresse-close";
	}

	@RequestMapping(value = "adresse-close.do", method = RequestMethod.POST)
	public String doCloseAddress(Model model, @Valid @ModelAttribute("closeCommand") CloseAdresseView closeView, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return showCloseAddress(model, closeView.getIdAdresse(), closeView);
		}
		controllerUtils.checkAccesDossierEnEcriture(closeView.getIdTiers());
		adresseManager.fermerAdresse(closeView.getIdAdresse(), closeView.getDateFin());
		return "redirect:edit.do?id=" + closeView.getIdTiers();
	}

	@RequestMapping(value = "adresse-add.do", method = RequestMethod.GET)
	public String showAddAddress(Model model, @RequestParam(value = "numero") Long idTiers) {
		controllerUtils.checkAccesDossierEnEcriture(idTiers);
		final AdresseView view = adresseManager.create(idTiers);
		return showAddAddress(model, view);
	}

	private String showAddAddress(Model model, AdresseView view) {
		model.addAttribute("editCommand", view);
		model.addAttribute("typeAdresseFiscaleTiers", tiersMapHelper.getMapTypeAdresseFiscale());
		model.addAttribute("textesCasePostale", tiersMapHelper.getMapTexteCasePostale());
		return "tiers/edition/adresse/adresse-edit";
	}

	@RequestMapping(value = "adresse-add.do", method = RequestMethod.POST)
	public String doAddAddress(Model model, @Valid @ModelAttribute("editCommand") AdresseView editView, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return showAddAddress(model, editView);
		}
		controllerUtils.checkAccesDossierEnEcriture(editView.getNumCTB());
		adresseManager.save(editView);
		return "redirect:edit.do?id=" + editView.getNumCTB();
	}
}