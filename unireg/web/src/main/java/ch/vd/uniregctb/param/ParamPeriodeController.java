package ch.vd.uniregctb.param;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
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
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.param.manager.ParamPeriodeManager;
import ch.vd.uniregctb.param.view.ModeleDocumentView;
import ch.vd.uniregctb.param.view.ParametrePeriodeFiscaleView;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityCheck;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.utils.RegDateEditor;

@Controller
@RequestMapping("/param/periode")
public class ParamPeriodeController {

	private static final String ACCESS_DENIED_MESSAGE = "Vous ne possédez aucun droit IfoSec sur l'écran de paramétrisation des périodes";

	private static final String PARAMETER_PERIODE_ID = "pf";
	private static final String PARAMETER_MODELE_ID = "md";

	private ParamPeriodeManager manager;
	private TiersMapHelper tiersMapHelper;
	private MessageSource messageSource;

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

	@InitBinder
	public void initBinder(WebDataBinder binder) {
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
		model.addAttribute("codeControleSurSommationDI", periodeSelectionnee.isShowCodeControleSommationDeclaration());
		model.addAttribute("parametrePeriodeFiscaleVaud", manager.getVaudByPeriodeFiscale(periodeSelectionnee));
		model.addAttribute("parametrePeriodeFiscaleDepense", manager.getDepenseByPeriodeFiscale(periodeSelectionnee));
		model.addAttribute("parametrePeriodeFiscaleHorsCanton", manager.getHorsCantonByPeriodeFiscale(periodeSelectionnee));
		model.addAttribute("parametrePeriodeFiscaleHorsSuisse", manager.getHorsSuisseByPeriodeFiscale(periodeSelectionnee));
		model.addAttribute("parametrePeriodeFiscaleDiplomateSuisse", manager.getDiplomateSuisseByPeriodeFiscale(periodeSelectionnee));

		final List<ModeleDocument> modeles = new ArrayList<>(manager.getModeleDocuments(periodeSelectionnee));
		Collections.sort(modeles, new Comparator<ModeleDocument>() {
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
			Collections.sort(feuilles, new ModeleFeuilleDocumentComparator());
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

	@RequestMapping(value = "/pf-edit.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.PARAM_PERIODE}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String showEditPeriod(Model model, @RequestParam(value = PARAMETER_PERIODE_ID) Long pfId) {
		final ParametrePeriodeFiscaleView view = manager.createParametrePeriodeFiscaleViewEdit(pfId);
		model.addAttribute("command", view);
		return "param/parametres-pf-edit";
	}

	@RequestMapping(value = "/pf-edit.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.PARAM_PERIODE}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String commitEditPeriod(Model model, @ModelAttribute("command") ParametrePeriodeFiscaleView view, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return showEditPeriod(model, view.getIdPeriodeFiscale());
		}
		manager.saveParametrePeriodeFiscaleView(view);
		return "redirect:/param/periode/list.do?pf=" + view.getIdPeriodeFiscale();
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
	public String postAddModel(Model model, @ModelAttribute("command") ModeleDocumentView view, BindingResult bindingResult) {
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
