package ch.vd.uniregctb.param;

import static ch.vd.uniregctb.param.Commun.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.param.manager.ParamPeriodeManager;
import ch.vd.uniregctb.param.view.ModeleFeuilleDocumentView;

public class ParamModeleFeuilleDocumentEditController extends AbstractSimpleFormController {

	private ParamPeriodeManager manager;
	
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		verifieLesDroits();
		return manager.createModeleFeuilleDocumentViewEdit(
				getPeriodeIdFromRequest(request),
				getModeleIdFromRequest(request),
				getFeuilleIdFromRequest(request));
	}
	
	
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {
		verifieLesDroits();
		ModeleFeuilleDocumentView mfdv = (ModeleFeuilleDocumentView) command;
		manager.saveModeleFeuilleDocumentViewEdit(mfdv);
		return getModelAndViewToPeriode(
				getPeriodeIdFromRequest(request), 
				getModeleIdFromRequest(request),
				true
		);
	}


	public void setManager(ParamPeriodeManager manager) {
		this.manager = manager;
	}


	
}
