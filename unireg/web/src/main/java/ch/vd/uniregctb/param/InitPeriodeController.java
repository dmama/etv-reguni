package ch.vd.uniregctb.param;

import static ch.vd.uniregctb.param.Commun.getModelAndViewToPeriode;
import static ch.vd.uniregctb.param.Commun.verifieLesDroits;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import ch.vd.uniregctb.param.manager.ParamPeriodeManager;


public class InitPeriodeController extends AbstractController {

	private ParamPeriodeManager manager;
	
	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		verifieLesDroits();
		
		// Creation de la periode
		manager.initNouvellePeriodeFiscale();

		// Reboucle vers l'écran période 
		return getModelAndViewToPeriode();
	}
	
	public void setManager(ParamPeriodeManager manager) {
		this.manager = manager;
	}

}
