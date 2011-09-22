package ch.vd.uniregctb.param;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocumentDAO;
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
	private ModeleFeuilleDocumentDAO modeleFeuilleDocumentDAO;

	public void setManager(ParamPeriodeManager manager) {
		this.manager = manager;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setModeleFeuilleDocumentDAO(ModeleFeuilleDocumentDAO modeleFeuilleDocumentDAO) {
		this.modeleFeuilleDocumentDAO = modeleFeuilleDocumentDAO;
	}

	@RequestMapping(value = "/add.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String add(@RequestParam("pf") Long periodeId, @RequestParam("md") Long modeleId, Model model) throws Exception {
		Commun.verifieLesDroits();

		final ModeleFeuilleDocumentView view = manager.createModeleFeuilleDocumentViewAdd(periodeId, modeleId);
		model.addAttribute("command", view);

		return "param/feuille-add";
	}

	@RequestMapping(value = "/add.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String add(@Valid ModeleFeuilleDocumentView view) throws Exception {
		Commun.verifieLesDroits();

		manager.addFeuille(view.getIdModele(), view.getModeleFeuille());

		return "redirect:/param/periode.do?pf=" + view.getIdPeriode() + "&md=" + view.getIdModele();
	}

	@RequestMapping(value = "/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String edit(@RequestParam("pf") Long periodeId, @RequestParam("md") Long modeleId, @RequestParam("mfd") Long feuilleId, Model model) throws Exception {
		Commun.verifieLesDroits();

		final ModeleFeuilleDocumentView view = manager.createModeleFeuilleDocumentViewEdit(periodeId, modeleId, feuilleId);
		model.addAttribute("command", view);

		return "param/feuille-edit";
	}

	@RequestMapping(value = "/edit.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String edit(@Valid ModeleFeuilleDocumentView view) throws Exception {
		Commun.verifieLesDroits();

		manager.updateFeuille(view.getIdFeuille(), view.getModeleFeuille());

		return "redirect:/param/periode.do?pf=" + view.getIdPeriode() + "&md=" + view.getIdModele();
	}

	@RequestMapping(value = "/suppr.do", method = RequestMethod.GET)
	@Transactional(rollbackFor = Throwable.class)
	public String suppr(@RequestParam("pf") Long periodeId, @RequestParam("md") Long modeleId, @RequestParam("mfd") Long feuilleId, Model model) throws Exception {
		Commun.verifieLesDroits();

		try {
			manager.deleteModeleFeuilleDocument(feuilleId);
		}
		catch (DataIntegrityViolationException e) {
			Map<Long, String> m = new HashMap<Long, String>(1);
			m.put(feuilleId, messageSource.getMessage("error.suppr.impossible", null, "error.suppr.impossible", Locale.getDefault()));
			model.addAttribute("error_feuille", m);
		}

		return "redirect:/param/periode.do?pf=" + periodeId + "&md=" + modeleId;
	}

	@RequestMapping(value = "/move.do", method = RequestMethod.GET)
	@Transactional(rollbackFor = Throwable.class)
	public String move(@RequestParam("mfd") Long feuilleId, @RequestParam("dir") Direction direction) throws Exception {
		Commun.verifieLesDroits();

		final ModeleFeuilleDocument feuille = modeleFeuilleDocumentDAO.get(feuilleId);
		if (feuille == null) {
			throw new ObjectNotFoundException("L'id spécifié ne correspond à aucune feuille. Aucune action effectuée.");
		}

		if (direction == null) {
			// rien à faire
			return "redirect:/param/periode.do?pf=" + feuille.getModeleDocument().getPeriodeFiscale().getId() + "&md=" + feuille.getModeleDocument().getId();
		}

		final ModeleDocument modele = feuille.getModeleDocument();
		Assert.notNull(modele);

		// on construit la liste des feuilles telle qu'ordonnée actuellement
		final List<ModeleFeuilleDocument> list = new ArrayList<ModeleFeuilleDocument>(modele.getModelesFeuilleDocument());
		Collections.sort(list, new ModeleFeuilleDocumentComparator());

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

		return "redirect:/param/periode.do?pf=" + feuille.getModeleDocument().getPeriodeFiscale().getId() + "&md=" + feuille.getModeleDocument().getId();
	}
}
