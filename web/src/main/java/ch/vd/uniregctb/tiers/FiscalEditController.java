package ch.vd.uniregctb.tiers;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.tiers.manager.ForFiscalManager;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.utils.HttpSessionConstants;
import ch.vd.uniregctb.utils.HttpSessionUtils;

@Controller
@RequestMapping("/fiscal")
public class FiscalEditController {

	private ControllerUtils controllerUtils;
	private ForFiscalManager forFiscalManager;

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setForFiscalManager(ForFiscalManager forFiscalManager) {
		this.forFiscalManager = forFiscalManager;
	}

	@SuppressWarnings("ConstantConditions")
	@RequestMapping(value = "/edit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String edit(@RequestParam(value = "id") long tiersId,
	                   @RequestParam(value = HttpSessionConstants.FORS_PRINCIPAUX_PAGINES, required = false) Boolean forsPrincipauxPagines,
	                   @RequestParam(value = HttpSessionConstants.FORS_SECONDAIRES_PAGINES, required = false) Boolean forsSecondairesPagines,
	                   @RequestParam(value = HttpSessionConstants.AUTRES_FORS_PAGINES, required = false) Boolean autresForsPrincipauxPagines,
	                   Model model, HttpServletRequest request) throws AdresseException {

		controllerUtils.checkAccesDossierEnEcriture(tiersId);
		final TiersEditView view = forFiscalManager.getView(tiersId);

		// la pagination des fors fiscaux est gérée par le composant display:table, on lui indique donc ici de quelle manière faire cette pagination.
		view.setForsPrincipauxPagines(HttpSessionUtils.getFromSession(request.getSession(), HttpSessionConstants.FORS_PRINCIPAUX_PAGINES, Boolean.class, Boolean.TRUE, forsPrincipauxPagines));
		view.setForsSecondairesPagines(HttpSessionUtils.getFromSession(request.getSession(), HttpSessionConstants.FORS_SECONDAIRES_PAGINES, Boolean.class, Boolean.TRUE, forsSecondairesPagines));
		view.setAutresForsPagines(HttpSessionUtils.getFromSession(request.getSession(), HttpSessionConstants.AUTRES_FORS_PAGINES, Boolean.class, Boolean.TRUE, autresForsPrincipauxPagines));

		model.addAttribute("command", view);
		return "tiers/edition/fiscal/edit";
	}
}
