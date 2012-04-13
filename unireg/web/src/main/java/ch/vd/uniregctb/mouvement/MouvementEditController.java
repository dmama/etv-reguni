package ch.vd.uniregctb.mouvement;

import javax.validation.Valid;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
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
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.mouvement.view.MouvementDetailView;
import ch.vd.uniregctb.tache.manager.TacheListManager;

@Controller
@SessionAttributes("nouveauMouvement")
public class MouvementEditController extends AbstractMouvementController {

	protected static final Logger LOGGER = Logger.getLogger(MouvementEditController.class);

	private TacheListManager tacheListManager;

	@SuppressWarnings("UnusedDeclaration")
	public void setTacheListManager(TacheListManager tacheListManager) {
		this.tacheListManager = tacheListManager;
	}

	private Validator validator;
	@SuppressWarnings("UnusedDeclaration")
	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	@InitBinder("nouveauMouvement")
	protected final void initBinder(WebDataBinder binder) {
		binder.setValidator(validator);
	}

	@RequestMapping(value ="/mouvement/edit.do", method = RequestMethod.GET)
	public ModelAndView get(@RequestParam("numero")Long idCtb, @RequestParam(value = "idTacheTraite", required = false) Long idTache, ModelMap model) throws Exception {
		ControllerUtils.checkAccesDossierEnLecture(idCtb);
		MouvementDetailView mvtDetailView;
		if (idTache != null) {
			mvtDetailView = mouvementEditManager.creerMvtForTacheTransmissionDossier(idCtb, idTache);
		}
		else {
			mvtDetailView = mouvementEditManager.creerMvt(idCtb);
		}
		model.put("nouveauMouvement", mvtDetailView);
		return new ModelAndView("mouvement/edit", model);
	}

	@RequestMapping(value ="/mouvement/edit.do", method = RequestMethod.POST)
	protected ModelAndView post(@ModelAttribute("nouveauMouvement") @Valid MouvementDetailView nouveauMouvementInSession, BindingResult result, ModelMap model) throws Exception {

		if (result.hasErrors()) {
			model.put("nouveauMouvement", nouveauMouvementInSession);
			return new ModelAndView("mouvement/edit", model);
		}

		ControllerUtils.checkAccesDossierEnEcriture(nouveauMouvementInSession.getContribuable().getNumero());
		mouvementEditManager.save(nouveauMouvementInSession);
		if (nouveauMouvementInSession.getIdTache() == null) {
			return new ModelAndView("redirect:edit-contribuable.do?numero=" + nouveauMouvementInSession.getContribuable().getNumero());
		}
		else {
			tacheListManager.traiteTache(nouveauMouvementInSession.getIdTache());
			return new ModelAndView("redirect:../tache/list.do");
		}
	}
}
