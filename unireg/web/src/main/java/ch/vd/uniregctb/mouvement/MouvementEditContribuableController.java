package ch.vd.uniregctb.mouvement;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.mouvement.manager.MouvementEditManager;

@Controller
public class MouvementEditContribuableController extends AbstractMouvementController {

	private MouvementEditManager mouvementEditManager;

	@SuppressWarnings("UnusedDeclaration")
	public MouvementEditManager getMouvementEditManager() {
		return mouvementEditManager;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setMouvementEditManager(MouvementEditManager mouvementEditManager) {
		this.mouvementEditManager = mouvementEditManager;
	}

	@RequestMapping(value = "/mouvement/edit-contribuable.do", method = RequestMethod.GET)
	protected ModelAndView get(@RequestParam("numero") Long id) throws Exception {
		ControllerUtils.checkAccesDossierEnLecture(id);
		return new ModelAndView("/mouvement/edit-contribuable", "command", mouvementEditManager.findByNumeroDossier(id, true));
	}

	@RequestMapping(value = "/mouvement/annuler.do", method = RequestMethod.POST)
	protected String post(@RequestParam("idMvt") Long idMvt) throws Exception {
		final long numCtb = mouvementEditManager.getNumeroContribuable(idMvt);
		ControllerUtils.checkAccesDossierEnEcriture(numCtb);
		mouvementEditManager.annulerMvt(idMvt);
		return "redirect:edit-contribuable.do?numero=" + numCtb;
	}
}
