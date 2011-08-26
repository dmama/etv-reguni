package ch.vd.uniregctb.param;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.uniregctb.param.manager.ParamPeriodeManager;
import ch.vd.uniregctb.param.view.ModeleFeuilleDocumentView;

/**
 * Ce contrôleur est responsable de l'ajout, de l'édition et de la suppression des feuilles sur les modèles de documents.
 */
@Controller
@RequestMapping(value = "/param/feuille")
public class ModeleFeuilleDocumentController {

	private ParamPeriodeManager manager;
	private MessageSource messageSource;

	public void setManager(ParamPeriodeManager manager) {
		this.manager = manager;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@RequestMapping(value = "/add", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String add(@RequestParam("pf") Long periodeId, @RequestParam("md") Long modeleId, Model model) throws Exception {
		Commun.verifieLesDroits();

		final ModeleFeuilleDocumentView view = manager.createModeleFeuilleDocumentViewAdd(periodeId, modeleId);
		model.addAttribute("command", view);

		return "param/feuille-add";
	}

	@RequestMapping(value = "/add", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String add(@Valid ModeleFeuilleDocumentView view) throws Exception {
		Commun.verifieLesDroits();

		manager.addFeuille(view.getIdModele(), view.getModeleFeuille());

		return "redirect:/param/periode.do?pf=" + view.getIdPeriode() + "&md=" + view.getIdModele();
	}

	@RequestMapping(value = "/edit", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String edit(@RequestParam("pf") Long periodeId, @RequestParam("md") Long modeleId, @RequestParam("mfd") Long feuilleId, Model model) throws Exception {
		Commun.verifieLesDroits();

		final ModeleFeuilleDocumentView view = manager.createModeleFeuilleDocumentViewEdit(periodeId, modeleId, feuilleId);
		model.addAttribute("command", view);

		return "param/feuille-edit";
	}

	@RequestMapping(value = "/edit", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String edit(@Valid ModeleFeuilleDocumentView view) throws Exception {
		Commun.verifieLesDroits();

		manager.updateFeuille(view.getIdFeuille(), view.getModeleFeuille());

		return "redirect:/param/periode.do?pf=" + view.getIdPeriode() + "&md=" + view.getIdModele();
	}

	@RequestMapping(value = "/suppr", method = RequestMethod.GET)
	@Transactional(rollbackFor = Throwable.class)
	public String suppr(@RequestParam("pf") Long periodeId, @RequestParam("md") Long modeleId, @RequestParam("mfd") Long feuilleId, Model model) throws Exception {
		Commun.verifieLesDroits();

		try {
			manager.deleteModeleFeuilleDocument(feuilleId);
		} catch (DataIntegrityViolationException e) {
			Map<Long, String> m = new HashMap<Long, String>(1);
			m.put(feuilleId, messageSource.getMessage("error.suppr.impossible", null, "error.suppr.impossible", Locale.getDefault()));
			model.addAttribute("error_feuille", m);
		}

		return "redirect:/param/periode.do?pf=" + periodeId + "&md=" + modeleId;
	}
}
