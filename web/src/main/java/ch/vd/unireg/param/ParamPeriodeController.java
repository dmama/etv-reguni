package ch.vd.unireg.param;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
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
import ch.vd.unireg.common.DynamicDelegatingValidator;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.ModeleFeuilleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.param.manager.ParamPeriodeManager;
import ch.vd.unireg.param.view.ModeleDocumentView;
import ch.vd.unireg.param.view.ParametrePeriodeFiscalePMEditView;
import ch.vd.unireg.param.view.ParametrePeriodeFiscalePPEditView;
import ch.vd.unireg.param.view.ParametrePeriodeFiscaleSNCEditView;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityCheck;
import ch.vd.unireg.tiers.TiersMapHelper;
import ch.vd.unireg.utils.RegDateEditor;

@Controller
@RequestMapping("/param/periode")
public class ParamPeriodeController {

	private static final String ACCESS_DENIED_MESSAGE = "Vous ne possédez aucun droit IfoSec sur l'écran de paramétrisation des périodes";

	private static final String PARAMETER_PERIODE_ID = "pf";
	private static final String PARAMETER_MODELE_ID = "md";

	private ParamPeriodeManager manager;
	private TiersMapHelper tiersMapHelper;
	private MessageSource messageSource;
	private DynamicDelegatingValidator validator;

	private static PeriodeFiscale findPeriodById(List<PeriodeFiscale> periodes, long id) {
		for (PeriodeFiscale pf : periodes) {
			if (pf.getId() == id) {
				return pf;
			}
		}
		return null;
	}

	private static ModeleDocument findModelById(List<ModeleDocument> modeles, long id) {
		for (ModeleDocument md : modeles) {
			if (md.getId() == id) {
				return md;
			}
		}
		return null;
	}

	public void setManager(ParamPeriodeManager manager) {
		this.manager = manager;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setValidators(Validator[] validators) {
		this.validator = new DynamicDelegatingValidator(validators);
	}

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		if (binder.getTarget() != null) {
			final List<Validator> supportingValidators = validator.getSupportingValidators(binder.getTarget().getClass());
			if (!supportingValidators.isEmpty()) {
				binder.addValidators(supportingValidators.toArray(new Validator[supportingValidators.size()]));
			}
		}
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(false, false, false, RegDateHelper.StringFormat.DISPLAY));
	}

	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.PARAM_PERIODE}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String view(Model model, HttpSession session,
	                   @RequestParam(value = PARAMETER_PERIODE_ID, required = false) Long pfId,
	                   @RequestParam(value = PARAMETER_MODELE_ID, required = false) Long modeleId) {

		final List<PeriodeFiscale> periodes = manager.getAllPeriodeFiscale();
		if (periodes.isEmpty()) {
			return "param/aucune-periode";
		}
		model.addAttribute("periodes", periodes);

		final PeriodeFiscale periodeDemandee = pfId != null ? findPeriodById(periodes, pfId) : null;
		final PeriodeFiscale periodeSelectionnee = periodeDemandee != null ? periodeDemandee : periodes.get(0);
		model.addAttribute("periodeSelectionnee", periodeSelectionnee);

		model.addAttribute("codeControleSurSommationDIPP", periodeSelectionnee.isShowCodeControleSommationDeclarationPP());
		model.addAttribute("parametrePeriodeFiscalePPVaud", manager.getPPVaudByPeriodeFiscale(periodeSelectionnee));
		model.addAttribute("parametrePeriodeFiscalePPDepense", manager.getPPDepenseByPeriodeFiscale(periodeSelectionnee));
		model.addAttribute("parametrePeriodeFiscalePPHorsCanton", manager.getPPHorsCantonByPeriodeFiscale(periodeSelectionnee));
		model.addAttribute("parametrePeriodeFiscalePPHorsSuisse", manager.getPPHorsSuisseByPeriodeFiscale(periodeSelectionnee));
		model.addAttribute("parametrePeriodeFiscalePPDiplomateSuisse", manager.getPPDiplomateSuisseByPeriodeFiscale(periodeSelectionnee));

		model.addAttribute("codeControleSurSommationDIPM", periodeSelectionnee.isShowCodeControleSommationDeclarationPM());
		model.addAttribute("parametrePeriodeFiscalePMVaud", manager.getPMVaudByPeriodeFiscale(periodeSelectionnee));
		model.addAttribute("parametrePeriodeFiscalePMHorsCanton", manager.getPMHorsCantonByPeriodeFiscale(periodeSelectionnee));
		model.addAttribute("parametrePeriodeFiscalePMHorsSuisse", manager.getPMHorsSuisseByPeriodeFiscale(periodeSelectionnee));
		model.addAttribute("parametrePeriodeFiscalePMUtilitePublique", manager.getPMUtilitePubliqueByPeriodeFiscale(periodeSelectionnee));

		model.addAttribute("codeControleSurRappelQSNC", periodeSelectionnee.isShowCodeControleRappelQuestionnaireSNC());
		model.addAttribute("parametrePeriodeFiscaleSNC", manager.getSNCByPeriodeFiscale(periodeSelectionnee));

		model.addAttribute("parametrePeriodeFiscaleEmomulementSommationDIPP", manager.getEmolumentSommationDIPPByPeriodeFiscale(periodeSelectionnee));

		final List<ModeleDocument> modeles = new ArrayList<>(manager.getModeleDocuments(periodeSelectionnee));
		modeles.sort(new Comparator<ModeleDocument>() {
			@Override
			public int compare(ModeleDocument o1, ModeleDocument o2) {
				if (o1.getTypeDocument() == null && o2.getTypeDocument() == null) {
					return 0;
				}
				if (o1.getTypeDocument() == null) {
					return -1;
				}
				if (o2.getTypeDocument() == null) {
					return 1;
				}
				return o1.getTypeDocument().compareTo(o2.getTypeDocument());
			}
		});

		model.addAttribute("modeles", modeles);

		final ModeleDocument modeleDemande = modeleId != null ? findModelById(modeles, modeleId) : null;
		final ModeleDocument modeleSelectionne = modeleDemande != null || modeles.isEmpty() ? modeleDemande : modeles.get(0);
		model.addAttribute("modeleSelectionne", modeleSelectionne);
		if (modeleSelectionne != null) {
			final List<ModeleFeuilleDocument> feuilles = new ArrayList<>(manager.getModeleFeuilleDocuments(modeleSelectionne));
			feuilles.sort(new ModeleFeuilleDocumentComparator());
			model.addAttribute("feuilles", feuilles);
		}
		else {
			model.addAttribute("feuilles", null);
		}

		final Object errorModele = session.getAttribute("error_modele");
		if (errorModele != null) {
			model.addAttribute("error_modele", errorModele);
			session.setAttribute("error_modele", null);
		}

		final Object errorFeuille = session.getAttribute("error_feuille");
		if (errorFeuille != null) {
			model.addAttribute("error_feuille", errorFeuille);
			session.setAttribute("error_feuille", null);
		}
		return "param/periode";
	}

	@RequestMapping(value = "/init-periode.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.PARAM_PERIODE}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String initPeriode() {
		manager.initNouvellePeriodeFiscale();
		return "redirect:/param/periode/list.do";
	}

	@RequestMapping(value = "/pf-edit-pp.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.PARAM_PERIODE}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String showEditPeriodPP(Model model, @RequestParam(value = PARAMETER_PERIODE_ID) Long pfId) {
		final ParametrePeriodeFiscalePPEditView view = manager.createParametrePeriodeFiscalePPEditView(pfId);
		return showEditPeriodPP(model, view);
	}

	private String showEditPeriodPP(Model model, ParametrePeriodeFiscalePPEditView view) {
		model.addAttribute("command", view);
		return "param/parametres-pf-pp-edit";
	}

	@RequestMapping(value = "/pf-edit-pp.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.PARAM_PERIODE}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String commitEditPeriodPP(Model model, @Valid @ModelAttribute("command") ParametrePeriodeFiscalePPEditView view, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return showEditPeriodPP(model, view);
		}
		manager.saveParametrePeriodeFiscaleView(view);
		return "redirect:/param/periode/list.do?pf=" + view.getIdPeriodeFiscale();
	}

	@RequestMapping(value = "/pf-edit-pm.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.PARAM_PERIODE}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String showEditPeriodPM(Model model, @RequestParam(value = PARAMETER_PERIODE_ID) Long pfId) {
		final ParametrePeriodeFiscalePMEditView view = manager.createParametrePeriodeFiscalePMEditView(pfId);
		return showEditPeriodPM(model, view);
	}

	private String showEditPeriodPM(Model model, ParametrePeriodeFiscalePMEditView view) {
		model.addAttribute("command", view);
		model.addAttribute("referencesPourDelais", tiersMapHelper.getMapReferencesPourDelai());
		return "param/parametres-pf-pm-edit";
	}

	@RequestMapping(value = "/pf-edit-pm.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.PARAM_PERIODE}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String commitEditPeriodPM(Model model, @Valid @ModelAttribute("command") ParametrePeriodeFiscalePMEditView view, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return showEditPeriodPM(model, view);
		}
		manager.saveParametrePeriodeFiscaleView(view);
		return "redirect:/param/periode/list.do?pf=" + view.getIdPeriodeFiscale();
	}

	@RequestMapping(value = "/pf-edit-snc.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.PARAM_PERIODE}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String showEditPeriodSNC(Model model, @RequestParam(value = PARAMETER_PERIODE_ID) Long pfId) {
		final ParametrePeriodeFiscaleSNCEditView view = manager.createParametrePeriodeFiscaleSNCEditView(pfId);
		return showEditPeriodSNC(model, view);
	}

	private String showEditPeriodSNC(Model model, ParametrePeriodeFiscaleSNCEditView view) {
		model.addAttribute("command", view);
		return "param/parametres-pf-snc-edit";
	}

	@RequestMapping(value = "/pf-edit-snc.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.PARAM_PERIODE}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String commitEditPeriodSNC(Model model, @Valid @ModelAttribute("command") ParametrePeriodeFiscaleSNCEditView view, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return showEditPeriodSNC(model, view);
		}
		manager.saveParametrePeriodeFiscaleView(view);
		return "redirect:/param/periode/list.do?pf=" + view.getAnneePeriodeFiscale();
	}

	@RequestMapping(value = "/modele-add.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.PARAM_PERIODE}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String showAddModel(Model model, @RequestParam(value = PARAMETER_PERIODE_ID) Long pfId) {
		final ModeleDocumentView view = manager.createModeleDocumentViewAdd(pfId);
		model.addAttribute("command", view);
		model.addAttribute("typeDocuments", tiersMapHelper.getTypesDeclarationImpotPourParam());
		return "param/modele-add";
	}

	@RequestMapping(value = "/modele-add.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.PARAM_PERIODE}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String postAddModel(Model model, @Valid @ModelAttribute("command") ModeleDocumentView view, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return showAddModel(model, view.getIdPeriode());
		}
		manager.saveModeleDocumentView(view);
		return "redirect:/param/periode/list.do?pf=" + view.getIdPeriode();
	}

	@RequestMapping(value = "/modele-suppr.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.PARAM_PERIODE}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String annulerModele(HttpSession session, @RequestParam(PARAMETER_PERIODE_ID) Long pfId, @RequestParam(PARAMETER_MODELE_ID) Long mdId) {
		try {
			manager.deleteModeleDocument(mdId);
		}
		catch (DataIntegrityViolationException e) {
			final Map<Long, String> m = new HashMap<>(1);
			m.put(mdId, messageSource.getMessage("error.suppr.impossible", null, "error.suppr.impossible", Locale.getDefault()));
			session.setAttribute("error_modele", m);
		}
		return "redirect:/param/periode/list.do?pf=" + pfId;
	}
}
