package ch.vd.uniregctb.admin.evenementExterne;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springmodules.xt.ajax.web.servlet.AjaxModelAndView;

import ch.vd.uniregctb.web.xt.AbstractEnhancedSimpleFormController;

public class RapportTravailController extends AbstractEnhancedSimpleFormController {

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		return new QuittancementView();
	}

	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return super.handleRequest(request, response);
	}

	@Override
	protected ModelAndView onSubmit(Object command, BindException errors) throws Exception {
		return new AjaxModelAndView(this.getSuccessView(), errors);
	}
}
