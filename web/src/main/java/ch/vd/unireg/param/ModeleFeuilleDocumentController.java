package ch.vd.unireg.param;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
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

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.ModeleFeuilleDocument;
import ch.vd.unireg.declaration.ModeleFeuilleDocumentDAO;
import ch.vd.unireg.param.manager.ParamPeriodeManager;
import ch.vd.unireg.param.view.ModeleFeuilleDocumentView;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityCheck;
import ch.vd.unireg.type.ModeleFeuille;

/**
 * Ce contrôleur est responsable de l'ajout, de l'édition et de la suppression des feuilles sur les modèles de documents.
 */
@Controller
@RequestMapping(value = "/param/periode/feuille")
public class ModeleFeuilleDocumentController {

	private static final String ACCESS_DENIED_MESSAGE = "Vous ne possédez aucun droit IfoSec sur l'écran de paramétrisation des périodes";

	private ParamPeriodeManager manager;
	private MessageSource messageSource;
	private ModeleFeuilleDocumentDAO modeleFeuilleDocumentDAO;
	private Validator modeleFeuilleDocumentValidator;

	public void setManager(ParamPeriodeManager manager) {
		this.manager = manager;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setModeleFeuilleDocumentDAO(ModeleFeuilleDocumentDAO modeleFeuilleDocumentDAO) {
		this.modeleFeuilleDocumentDAO = modeleFeuilleDocumentDAO;
	}

	public void setModeleFeuilleDocumentValidator(Validator modeleFeuilleDocumentValidator) {
		this.modeleFeuilleDocumentValidator = modeleFeuilleDocumentValidator;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(modeleFeuilleDocumentValidator);
	}

	@RequestMapping(value = "/add.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@SecurityCheck(rolesToCheck = {Role.PARAM_PERIODE}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String add(@RequestParam("pf") Long periodeId, @RequestParam("md") Long modeleId, Model model) throws Exception {

		final ModeleFeuilleDocumentView view = manager.createModeleFeuilleDocumentViewAdd(periodeId, modeleId);
		model.addAttribute("command", view);
		model.addAttribute("modelesFeuilles", ModeleFeuille.forDocument(view.getModeleDocumentTypeDocument()));

		return "param/feuille-add";
	}

	@RequestMapping(value = "/add.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	@SecurityCheck(rolesToCheck = {Role.PARAM_PERIODE}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String add(Model model, @Valid @ModelAttribute("command") ModeleFeuilleDocumentView view, BindingResult result) throws Exception {
		if (result.hasErrors()) {
			model.addAttribute("modelesFeuilles", ModeleFeuille.forDocument(view.getModeleDocumentTypeDocument()));
			return "param/feuille-add";
		}
		manager.addFeuille(view.getIdModele(), view.getModeleFeuille());

		return "redirect:/param/periode/list.do?pf=" + view.getIdPeriode() + "&md=" + view.getIdModele();
	}

	@RequestMapping(value = "/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@SecurityCheck(rolesToCheck = {Role.PARAM_PERIODE}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String edit(@RequestParam("pf") Long periodeId, @RequestParam("md") Long modeleId, @RequestParam("mfd") Long feuilleId, Model model) throws Exception {
		final ModeleFeuilleDocumentView view = manager.createModeleFeuilleDocumentViewEdit(periodeId, modeleId, feuilleId);
		model.addAttribute("command", view);
		model.addAttribute("modelesFeuilles", ModeleFeuille.forDocument(view.getModeleDocumentTypeDocument()));
		return "param/feuille-edit";
	}

	@RequestMapping(value = "/edit.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	@SecurityCheck(rolesToCheck = {Role.PARAM_PERIODE}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String edit(Model model, @Valid @ModelAttribute("command") ModeleFeuilleDocumentView view, BindingResult result) throws Exception {
		if (result.hasErrors()) {
			model.addAttribute("modelesFeuilles", ModeleFeuille.forDocument(view.getModeleDocumentTypeDocument()));
			return "param/feuille-edit";
		}
		manager.updateFeuille(view.getIdFeuille(), view.getModeleFeuille());

		return "redirect:/param/periode/list.do?pf=" + view.getIdPeriode() + "&md=" + view.getIdModele();
	}

	@RequestMapping(value = "/suppr.do", method = RequestMethod.GET)
	@Transactional(rollbackFor = Throwable.class)
	@SecurityCheck(rolesToCheck = {Role.PARAM_PERIODE}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String suppr(@RequestParam("pf") Long periodeId, @RequestParam("md") Long modeleId, @RequestParam("mfd") Long feuilleId, Model model) throws Exception {
		try {
			manager.deleteModeleFeuilleDocument(feuilleId);
		}
		catch (DataIntegrityViolationException e) {
			Map<Long, String> m = new HashMap<>(1);
			m.put(feuilleId, messageSource.getMessage("error.suppr.impossible", null, "error.suppr.impossible", Locale.getDefault()));
			model.addAttribute("error_feuille", m);
		}

		return "redirect:/param/periode/list.do?pf=" + periodeId + "&md=" + modeleId;
	}

	@RequestMapping(value = "/move.do", method = RequestMethod.GET)
	@Transactional(rollbackFor = Throwable.class)
	@SecurityCheck(rolesToCheck = {Role.PARAM_PERIODE}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	public String move(@RequestParam("mfd") Long feuilleId, @RequestParam("dir") Direction direction) throws Exception {
		final ModeleFeuilleDocument feuille = modeleFeuilleDocumentDAO.get(feuilleId);
		if (feuille == null) {
			throw new ObjectNotFoundException("L'id spécifié ne correspond à aucune feuille. Aucune action effectuée.");
		}

		if (direction == null) {
			// rien à faire
			return "redirect:/param/periode/list.do?pf=" + feuille.getModeleDocument().getPeriodeFiscale().getId() + "&md=" + feuille.getModeleDocument().getId();
		}

		final ModeleDocument modele = feuille.getModeleDocument();
		Assert.notNull(modele);

		// on construit la liste des feuilles telle qu'ordonnée actuellement
		final List<ModeleFeuilleDocument> list = new ArrayList<>(modele.getModelesFeuilleDocument());
		list.sort(new ModeleFeuilleDocumentComparator());

		// on décale d'un cran vers le haut ou le bas la feuille spécifiée
		for (int i = 0; i < list.size(); i++) {
			final ModeleFeuilleDocument f = list.get(i);
			if (f == feuille) {
				if (direction == Direction.UP) {
					if (i == 0) {
						// déjà en première position : rien à faire
					}
					else {
						// on remonte la feuille un cran plus haut
						list.add(i - 1, list.remove(i));
					}
				}
				else {
					if (i == list.size() - 1) {
						// déjà en dernière position : rien à faire
					}
					else {
						// on descend la feuille un cran plus bas
						list.add(i + 1, list.remove(i));
					}
				}
				break;
			}
		}

		// on réinitialise tous les indexes pour être sûr de ne pas avoir de doublons ou de trous
		for (int i = 0; i < list.size(); i++) {
			final ModeleFeuilleDocument f = list.get(i);
			f.setIndex(i);
		}

		return "redirect:/param/periode/list.do?pf=" + feuille.getModeleDocument().getPeriodeFiscale().getId() + "&md=" + feuille.getModeleDocument().getId();
	}
}
