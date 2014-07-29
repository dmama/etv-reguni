package ch.vd.uniregctb.tiers;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
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
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.manager.TiersEditManager;
import ch.vd.uniregctb.tiers.view.DebiteurEditView;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.utils.RegDateEditor;

@Controller
@RequestMapping(value = "/debiteur")
public class DebiteurEditController {

	public static final String LIBELLES_LOGICIELS = "libellesLogiciel";
	public static final String CATEGORIES_IMPOT_SOURCE = "categoriesImpotSource";
	public static final String MODES_COMMUNICATION = "modesCommunication";
	public static final String PERIODES_DECOMPTE = "periodesDecomptes";

	private static final String ID = "id";
	private static final String NOUVELLE_PERIODICITE = "nouvellePeriodicite";
	private static final String PERIODICITE_COURANTE = "periodicite";
	private static final String CATEGORIE_IMPOT_SOURCE_COURANTE = "categorieImpotSource";

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

	@RequestMapping(value = "/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true)
	public String getDebiteurToEdit(Model model, @RequestParam(value = ID) long id) throws Exception {
		controllerUtils.checkAccesDossierEnLecture(id);
		final DebiteurEditView view = tiersEditManager.getDebiteurEditView(id);

		model.addAttribute(PERIODES_DECOMPTE, tiersMapHelper.getPeriodeDecomptes());
		model.addAttribute(MODES_COMMUNICATION, tiersMapHelper.getMapModeCommunication());
		model.addAttribute(LIBELLES_LOGICIELS, tiersMapHelper.getAllLibellesLogiciels());
		model.addAttribute(CATEGORIES_IMPOT_SOURCE, tiersMapHelper.getMapCategorieImpotSource());
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

	@RequestMapping(value = "/periodicites-autorisees.do", method = RequestMethod.GET)
	@ResponseBody
	public Map<PeriodiciteDecompte, String> getPeriodicitesAutorisees(@RequestParam(value = PERIODICITE_COURANTE, required = false) PeriodiciteDecompte periodiciteCourante,
	                                                                  @RequestParam(value = CATEGORIE_IMPOT_SOURCE_COURANTE) CategorieImpotSource cis) {
		return getMapPeriodicites(periodiciteCourante, cis, hasFullRights(), tiersMapHelper);
	}

	/**
	 * Externalisé pour le test
	 * @param periodiciteCourante la périodicité actuellement connue du débiteur (<code>null</code> en mode création)
	 * @param cis la catégorie impôt source du débiteur
	 * @param fullRights <code>true</code> si l'utilisateur a le rôle SuperGRA
	 * @param tiersMapHelper le helper qui va bien
	 * @return une map des périodicités autorisées dans ce cas (la valeur est le libellé à afficher) triées par ordre alphabétique des libellés
	 */
	protected static Map<PeriodiciteDecompte, String> getMapPeriodicites(@Nullable PeriodiciteDecompte periodiciteCourante,
	                                                                     CategorieImpotSource cis,
	                                                                     boolean fullRights,
	                                                                     TiersMapHelper tiersMapHelper) {
		final Map<PeriodiciteDecompte, String> map;
		if (fullRights) {
			map = tiersMapHelper.getMapPeriodiciteDecompte();
		}
		else {
			map = tiersMapHelper.getMapLimiteePeriodiciteDecompte(periodiciteCourante, cis);
		}
		return map;
	}

	@RequestMapping(value = "/edit.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String saveDebiteur(@ModelAttribute("command") DebiteurEditView view) throws Exception {
		controllerUtils.checkAccesDossierEnEcriture(view.getId());
		tiersEditManager.save(view);
		return "redirect:/tiers/visu.do?id=" + view.getId();
	}
}
