package ch.vd.uniregctb.tiers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractCommandController;

import ch.vd.uniregctb.tiers.view.TiersVignetteView;

public class TiersVignetteController extends AbstractCommandController {

	@Override
	protected ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {
		final TiersVignetteView view = (TiersVignetteView) command;
		view.init(request);
		return new ModelAndView("tiers/visualisation/vignette", errors.getModel());
	}
}
