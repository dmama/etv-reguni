package ch.vd.uniregctb.param;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import ch.vd.uniregctb.param.manager.ParamPeriodeManager;
import ch.vd.uniregctb.security.SecurityProviderInterface;

import static ch.vd.uniregctb.param.Commun.getModelAndViewToPeriode;
import static ch.vd.uniregctb.param.Commun.verifieLesDroits;


public class InitPeriodeController extends AbstractController {

	private ParamPeriodeManager manager;
	private SecurityProviderInterface securityProvider;

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		verifieLesDroits(securityProvider);
		
		// Creation de la periode
		manager.initNouvellePeriodeFiscale();

		// Reboucle vers l'écran période 
		return getModelAndViewToPeriode();
	}
	
	public void setManager(ParamPeriodeManager manager) {
		this.manager = manager;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}
}
