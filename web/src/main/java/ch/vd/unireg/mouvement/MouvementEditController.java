package ch.vd.unireg.mouvement;

import javax.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

import ch.vd.unireg.mouvement.view.MouvementDetailView;
import ch.vd.unireg.tache.manager.TacheListManager;

@Controller
@RequestMapping(value = "/mouvement")
@SessionAttributes(MouvementEditController.NOUVEAU_MOUVEMENT_NAME)
public class MouvementEditController extends AbstractMouvementController {

	public static final String NOUVEAU_MOUVEMENT_NAME = "nouveauMouvement";

	private TacheListManager tacheListManager;
	private Validator validator;

	@SuppressWarnings("UnusedDeclaration")
	public void setTacheListManager(TacheListManager tacheListManager) {
		this.tacheListManager = tacheListManager;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	@InitBinder(NOUVEAU_MOUVEMENT_NAME)
	protected final void initBinder(WebDataBinder binder) {
		binder.setValidator(validator);
	}

	@RequestMapping(value ="/edit.do", method = RequestMethod.GET)
	public String get(@RequestParam("numero")Long idCtb, @RequestParam(value = "idTacheTraite", required = false) Long idTache, ModelMap model) throws Exception {
		controllerUtils.checkAccesDossierEnLecture(idCtb);
		MouvementDetailView mvtDetailView;
		if (idTache != null) {
			mvtDetailView = mouvementEditManager.creerMvtForTacheTransmissionDossier(idCtb, idTache);
		}
		else {
			mvtDetailView = mouvementEditManager.creerMvt(idCtb);
		}
		model.put(NOUVEAU_MOUVEMENT_NAME, mvtDetailView);
		return "/mouvement/edit";
	}

	@RequestMapping(value ="/edit.do", method = RequestMethod.POST)
	public String post(@ModelAttribute(NOUVEAU_MOUVEMENT_NAME) @Valid MouvementDetailView nouveauMouvementInSession, BindingResult result, ModelMap model) throws Exception {

		if (result.hasErrors()) {
			model.put(NOUVEAU_MOUVEMENT_NAME, nouveauMouvementInSession);
			return "/mouvement/edit";
		}

		controllerUtils.checkAccesDossierEnEcriture(nouveauMouvementInSession.getContribuable().getNumero());
		mouvementEditManager.save(nouveauMouvementInSession);
		if (nouveauMouvementInSession.getIdTache() == null) {
			return String.format("redirect:edit-contribuable.do?numero=%d", nouveauMouvementInSession.getContribuable().getNumero());
		}
		else {
			tacheListManager.traiteTache(nouveauMouvementInSession.getIdTache());
			return "redirect:../tache/list.do";
		}
	}

	@RequestMapping(value = "/edit-contribuable.do", method = RequestMethod.GET)
	public String get(Model model, @RequestParam("numero") Long id) throws Exception {
		controllerUtils.checkAccesDossierEnLecture(id);
		model.addAttribute("command", mouvementEditManager.findByNumeroDossier(id, true));
		return "/mouvement/edit-contribuable";
	}

	@RequestMapping(value = "/annuler.do", method = RequestMethod.POST)
	public String post(@RequestParam("idMvt") Long idMvt) throws Exception {
		final long numCtb = mouvementEditManager.getNumeroContribuable(idMvt);
		controllerUtils.checkAccesDossierEnEcriture(numCtb);
		mouvementEditManager.annulerMvt(idMvt);
		return String.format("redirect:edit-contribuable.do?numero=%d", numCtb);
	}
}
