package ch.vd.unireg.param.online;

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
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.Flash;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.URLHelper;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.parametrage.ParametreDemandeDelaisOnline;
import ch.vd.unireg.parametrage.ParametrePeriodeFiscaleDAO;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityCheck;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.DayMonthHelper;
import ch.vd.unireg.utils.DayMonthEditor;
import ch.vd.unireg.utils.RegDateEditor;

@Controller
@RequestMapping("/param/periode/online")
public class ParamDelaisOnlineController {

	private static final String ACCESS_DENIED_MESSAGE = "Vous ne possédez aucun droit IfoSec sur l'écran de paramétrisation des périodes";
	private static final String PARAMETER_PERIODE_ID = "pf";

	private PeriodeFiscaleDAO periodeFiscaleDAO;
	private ParametrePeriodeFiscaleDAO parametrePeriodeFiscaleDAO;
	private Validator delaisOnlinePPValidator;

	@InitBinder(value = "delaisPP")
	public void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(false, false, false, RegDateHelper.StringFormat.DISPLAY));
		binder.registerCustomEditor(DayMonth.class, new DayMonthEditor(true, false, DayMonthHelper.StringFormat.DISPLAY));
		binder.setValidator(delaisOnlinePPValidator);
	}

	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@RequestMapping(value = "pp/edit.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.PARAM_PERIODE}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String ppEdit(Model model, @RequestParam(value = PARAMETER_PERIODE_ID) Long pfId) {

		final PeriodeFiscale pf = periodeFiscaleDAO.get(pfId);
		if (pf == null) {
			throw new ObjectNotFoundException("Impossible de retrouver la période fiscale id : " + pfId);
		}

		final ParametreDemandeDelaisOnline paramsDemandesDelaisOnlinePP = parametrePeriodeFiscaleDAO.getParametreDemandeDelaisOnline(pf.getAnnee(), ParametreDemandeDelaisOnline.Type.PP);
		if (paramsDemandesDelaisOnlinePP == null) {
			throw new ObjectNotFoundException("Il n'y a pas de paramètres pour les demandes de délais online des DIs PP sur la période fiscale " + pf.getAnnee());
		}

		model.addAttribute("delaisPP", new DelaisOnlinePPView(pf, paramsDemandesDelaisOnlinePP));
		return "param/online/pp/edit";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "pp/edit.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.PARAM_PERIODE}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String ppEdit(@Valid @ModelAttribute(name = "delaisPP") DelaisOnlinePPView view, BindingResult bindings, Model model) {

		final Long pfId = view.getPeriodeFiscaleId();
		final PeriodeFiscale pf = periodeFiscaleDAO.get(pfId);
		if (pf == null) {
			throw new ObjectNotFoundException("Impossible de retrouver la période fiscale id : " + pfId);
		}

		if (bindings.hasErrors()) {
			model.addAttribute("delaisPP", view);
			return "param/online/pp/edit";
		}

		final ParametreDemandeDelaisOnline paramsDemandesDelaisOnlinePP = parametrePeriodeFiscaleDAO.getParametreDemandeDelaisOnline(pf.getAnnee(), ParametreDemandeDelaisOnline.Type.PP);
		if (paramsDemandesDelaisOnlinePP == null) {
			throw new ObjectNotFoundException("Il n'y a pas de paramètres pour les demandes de délais online des DIs PP sur la période fiscale " + pf.getAnnee());
		}

		view.calculeDatesFin();
		view.copyTo(paramsDemandesDelaisOnlinePP);

		Flash.message("Les nouvelles valeurs de délais accordables en ligne des DIs PP ont été sauvées en base.");
		return URLHelper.navigateBackTo("/param/periode/list.do").defaultTo("/param/periode/list.do", "pf=" + pfId);
	}

	public void setPeriodeFiscaleDAO(PeriodeFiscaleDAO periodeFiscaleDAO) {
		this.periodeFiscaleDAO = periodeFiscaleDAO;
	}

	public void setParametrePeriodeFiscaleDAO(ParametrePeriodeFiscaleDAO parametrePeriodeFiscaleDAO) {
		this.parametrePeriodeFiscaleDAO = parametrePeriodeFiscaleDAO;
	}

	public void setDelaisOnlinePPValidator(Validator delaisOnlinePPValidator) {
		this.delaisOnlinePPValidator = delaisOnlinePPValidator;
	}
}
