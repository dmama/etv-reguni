package ch.vd.uniregctb.param;

import static ch.vd.uniregctb.param.Commun.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.param.manager.ParamPeriodeManager;
import ch.vd.uniregctb.param.view.ParametrePeriodeFiscaleView;

public class ParamParametrePeriodeFiscaleEditController extends AbstractSimpleFormController {

	private ParamPeriodeManager manager;
	
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		verifieLesDroits();
		return manager.createParametrePeriodeFiscaleViewEdit(getPeriodeIdFromRequest(request));
	}
	
	
	@Override	
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {
		
		verifieLesDroits();

		ParametrePeriodeFiscaleView ppfv = (ParametrePeriodeFiscaleView) command;
		manager.saveParametrePeriodeFiscaleView(ppfv);

		return getModelAndViewToPeriode(ppfv.getIdPeriodeFiscale(),true);
	}


	public void setManager(ParamPeriodeManager manager) {
		this.manager = manager;
	}


	
}
