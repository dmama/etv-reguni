package ch.vd.uniregctb.param;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.param.manager.ParamPeriodeManager;
import ch.vd.uniregctb.param.view.ParametrePeriodeFiscaleView;
import ch.vd.uniregctb.security.SecurityProviderInterface;

import static ch.vd.uniregctb.param.Commun.getModelAndViewToPeriode;
import static ch.vd.uniregctb.param.Commun.getPeriodeIdFromRequest;
import static ch.vd.uniregctb.param.Commun.verifieLesDroits;

public class ParamParametrePeriodeFiscaleEditController extends AbstractSimpleFormController {

	private ParamPeriodeManager manager;
	private SecurityProviderInterface securityProvider;

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		verifieLesDroits(securityProvider);
		return manager.createParametrePeriodeFiscaleViewEdit(getPeriodeIdFromRequest(request));
	}
	
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {
		
		verifieLesDroits(securityProvider);

		ParametrePeriodeFiscaleView ppfv = (ParametrePeriodeFiscaleView) command;
		manager.saveParametrePeriodeFiscaleView(ppfv);

		return getModelAndViewToPeriode(ppfv.getIdPeriodeFiscale());
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setManager(ParamPeriodeManager manager) {
		this.manager = manager;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}
}
