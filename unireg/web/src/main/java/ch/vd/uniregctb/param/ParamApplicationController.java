package ch.vd.uniregctb.param;

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

import ch.vd.uniregctb.param.manager.ParamApplicationManager;
import ch.vd.uniregctb.param.view.ParamApplicationView;
import ch.vd.uniregctb.parametrage.ParametreEnum;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityCheck;

@Controller
@RequestMapping("/param/app")
public class ParamApplicationController {

	private static final String ACCESS_DENIED_MESSAGE = "Vous ne possédez aucun droit IfoSec de gestion des paramètres";

	private ParamApplicationManager manager;
	private Validator validator;

	public void setParamApplicationManager(
			ParamApplicationManager paramApplicationManager) {
		this.manager = paramApplicationManager;
	}

	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.setValidator(validator);
	}

	@ModelAttribute
	protected void referenceData(Model model) throws Exception {
		for (ParametreEnum p : ParametreEnum.values()) {
			model.addAttribute(p.toString() + "ParDefaut", manager.getDefaut(p));
		}
	}

	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.PARAM_APP}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String list (Model model) {
		model.addAttribute("params", manager.getForm());
		return "param/application";
	}

	@RequestMapping(value = "/save.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.PARAM_APP}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String save(@Valid @ModelAttribute("params") ParamApplicationView form, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return "param/application";
		}
		manager.save(form);
		return "redirect:list.do";
	}

	@RequestMapping(value = "/reset.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.PARAM_APP}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String reset() {
		manager.reset();
		return "redirect:list.do";
	}
}
