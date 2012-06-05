package ch.vd.uniregctb.tiers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.tiers.manager.MouvementVisuManager;

/**
 * Controller pour l'overlay de visualisation du détail du mouvement
 *
 */
@Controller
public class MouvementVisuController {

	private MouvementVisuManager mouvementVisuManager;

	public void setMouvementVisuManager(MouvementVisuManager mouvementVisuManager) {
		this.mouvementVisuManager = mouvementVisuManager;
	}

	@RequestMapping("/tiers/mouvement.do")
	public ModelAndView mouvement(@RequestParam("idMvt") Long idMvt) {
		return new ModelAndView("tiers/visualisation/mouvement/mouvement", "command", mouvementVisuManager.get(idMvt));
	}

}
