package ch.vd.uniregctb.identification.contribuable.tooltip.adresse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractCommandController;

/**
 * Affiche la dernière adresse vaudoise connue d'un contribuable lorsque la souris est positionnée sur la localité/pays.
 */
public class IdentificationAdresseTooltipController extends AbstractCommandController {

	private IdentificationAdresseTooltipManager manager;

	public void setManager(IdentificationAdresseTooltipManager manager) {
		this.manager = manager;
	}

	@Override
	protected ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {
		final IdentificationAdresseTooltipView view = (IdentificationAdresseTooltipView) command;

		final Long noCtb = Long.parseLong(request.getParameter("noCtb"));
		manager.fillDerniereAdresseVaudoiseConnue(noCtb, view);

		return new ModelAndView("/identification/tooltip/adresse", errors.getModel());
	}
}
