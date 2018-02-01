package ch.vd.unireg.tiers;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.ControllerUtils;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.manager.TiersEditManager;
import ch.vd.unireg.tiers.view.DebiteurEditView;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.utils.RegDateEditor;

@Controller
@RequestMapping(value = "/debiteur")
public class DebiteurEditController {

	public static final String LIBELLES_LOGICIELS = "libellesLogiciel";
	public static final String CATEGORIES_IMPOT_SOURCE = "categoriesImpotSource";
	public static final String MODES_COMMUNICATION = "modesCommunication";
	public static final String PERIODES_DECOMPTE = "periodesDecomptes";
	public static final String PERIODICITES_DECOMPTE = "periodicitesDecomptes";

	private static final String ID = "id";
	private static final String NOUVELLE_PERIODICITE = "nouvellePeriodicite";

	private TiersMapHelper tiersMapHelper;
	private TiersEditManager tiersEditManager;
	private SecurityProviderInterface securityProvider;
	private ControllerUtils controllerUtils;

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setTiersEditManager(TiersEditManager tiersEditManager) {
		this.tiersEditManager = tiersEditManager;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	private boolean hasFullRights() {
		return SecurityHelper.isGranted(securityProvider, Role.SUPERGRA);
	}

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
	}

	private Map<PeriodiciteDecompte, String> getAllowedPeriodicitesDecomptes(PeriodiciteDecompte periodiciteActuelle) {
		final Map<PeriodiciteDecompte, String> map;
		if (hasFullRights()) {
			map = tiersMapHelper.getMapPeriodiciteDecompte();
		}
		else {
			map = tiersMapHelper.getMapLimiteePeriodiciteDecompte(periodiciteActuelle);
		}
		return map;
	}

	@RequestMapping(value = "/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true)
	public String getDebiteurToEdit(Model model, @RequestParam(value = ID) long id, @RequestParam(value = ControllerUtils.PARAMETER_MODIFIER, defaultValue = "false") boolean modified) throws Exception {
		controllerUtils.checkAccesDossierEnLecture(id);
		final DebiteurEditView view = tiersEditManager.getDebiteurEditView(id);

		model.addAttribute(PERIODICITES_DECOMPTE, getAllowedPeriodicitesDecomptes(view.getPeriodiciteActive()));
		model.addAttribute(PERIODES_DECOMPTE, tiersMapHelper.getPeriodeDecomptes());
		model.addAttribute(MODES_COMMUNICATION, tiersMapHelper.getMapModeCommunication());
		model.addAttribute(LIBELLES_LOGICIELS, tiersMapHelper.getAllLibellesLogiciels());
		model.addAttribute(CATEGORIES_IMPOT_SOURCE, tiersMapHelper.getMapCategorieImpotSource());
		model.addAttribute(ControllerUtils.PARAMETER_MODIFIER, modified);
		model.addAttribute("command", view);
		return "tiers/edition/debiteur/edit";
	}

	@RequestMapping(value = "/dates-nouvelle-periodicite.do", method = RequestMethod.GET)
	@ResponseBody
	public List<RegDate> getDatesNouvellePeriodicite(@RequestParam(value = ID) long dpiId,
	                                                 @RequestParam(value = NOUVELLE_PERIODICITE) PeriodiciteDecompte nouvellePeriodicite) {

		// max un an dans le futur
		final RegDate maxDate = RegDate.get().addYears(1);
		return tiersEditManager.getDatesPossiblesPourDebutNouvellePeriodicite(dpiId, nouvellePeriodicite, maxDate, !hasFullRights());
	}

	@RequestMapping(value = "/edit.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String saveDebiteur(@ModelAttribute("command") DebiteurEditView view) throws Exception {
		controllerUtils.checkAccesDossierEnEcriture(view.getId());
		tiersEditManager.save(view);
		return "redirect:/tiers/visu.do?id=" + view.getId();
	}
}
