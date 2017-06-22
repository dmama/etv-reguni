package ch.vd.uniregctb.situationfamille;

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
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.tiers.manager.SituationFamilleManager;
import ch.vd.uniregctb.tiers.view.SituationFamilleView;
import ch.vd.uniregctb.utils.RegDateEditor;

@Controller
@RequestMapping(value = "/situationfamille")
public class SituationFamilleController {

	private ControllerUtils controllerUtils;
	private SecurityProviderInterface securityProvider;
	private SituationFamilleManager situationFamilleManager;
	private TiersMapHelper tiersMapHelper;
	private Validator validator;

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.setValidator(validator);
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
	}

	@RequestMapping(value = "/add.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String add(@RequestParam(value = "tiersId") long tiersId, Model model) throws AdresseException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.SIT_FAM)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit IfoSec d'édition des situations de famille dans Unireg");
		}
		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		final SituationFamilleView view = situationFamilleManager.create(tiersId);
		model.addAttribute("command", view);
		model.addAttribute("etatCivil", tiersMapHelper.getMapEtatsCivil());
		model.addAttribute("tarifsImpotSource", tiersMapHelper.getTarifsImpotSource());
		return "tiers/edition/fiscal/situation-famille-edit";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/add.do", method = RequestMethod.POST)
	public String add(@Valid @ModelAttribute("command") final SituationFamilleView view, BindingResult result, Model model) throws Exception {

		final long id = view.getNumeroCtb();

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.SIT_FAM)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit IfoSec d'édition des situations de famille dans Unireg");
		}
		controllerUtils.checkAccesDossierEnEcriture(id);

		if (result.hasErrors()) {
			model.addAttribute("etatCivil", tiersMapHelper.getMapEtatsCivil());
			model.addAttribute("tarifsImpotSource", tiersMapHelper.getTarifsImpotSource());
			return "tiers/edition/fiscal/situation-famille-edit";
		}

		situationFamilleManager.save(view);
		return "redirect:/fiscal/edit.do?id=" + id;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setSituationFamilleManager(SituationFamilleManager situationFamilleManager) {
		this.situationFamilleManager = situationFamilleManager;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setValidator(Validator validator) {
		this.validator = validator;
	}
}
