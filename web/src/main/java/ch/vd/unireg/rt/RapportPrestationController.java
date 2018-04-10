package ch.vd.unireg.rt;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.unireg.common.ControllerUtils;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityCheck;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.manager.TiersVisuManager;
import ch.vd.unireg.tiers.view.RapportsPrestationView;

/**
 * Contrôleur qui gère les rapports de prestations entre débiteurs et sourciers.
 */
@Controller
public class RapportPrestationController {

	public static final String DROIT_CONSULTATION_DPI = "vous ne possédez pas le droit IfoSec de consultation des débiteurs de prestations imposables";

	private TiersVisuManager manager;
	private ControllerUtils controllerUtils;
	private SecurityProviderInterface securityProvider;

	/**
	 * Affiche la liste complète (non-paginée) des rapports de travail du débiteur spécifié.
	 *
	 * @param idDpi le numéro de tiers du débiteur
	 */
	@SecurityCheck(rolesToCheck = Role.VISU_ALL, accessDeniedMessage = DROIT_CONSULTATION_DPI)
	@RequestMapping(value = "/rapports-prestation/full-list.do", method = RequestMethod.GET)
	public String fullList(@RequestParam long idDpi, Model model) {

		controllerUtils.checkAccesDossierEnLecture(idDpi);

		final RapportsPrestationView bean = new RapportsPrestationView();
		manager.fillRapportsPrestationView(idDpi, bean);
		model.addAttribute("command", bean);

		return "tiers/visualisation/rt/list";
	}

	public void setManager(TiersVisuManager manager) {
		this.manager = manager;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}
}

