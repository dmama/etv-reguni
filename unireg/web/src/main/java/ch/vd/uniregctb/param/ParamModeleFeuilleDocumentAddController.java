package ch.vd.uniregctb.param;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.param.manager.ParamPeriodeManager;
import ch.vd.uniregctb.param.view.ModeleFeuilleDocumentView;

import static ch.vd.uniregctb.param.Commun.getModelAndViewToPeriode;
import static ch.vd.uniregctb.param.Commun.getModeleIdFromRequest;
import static ch.vd.uniregctb.param.Commun.getPeriodeIdFromRequest;
import static ch.vd.uniregctb.param.Commun.verifieLesDroits;

public class ParamModeleFeuilleDocumentAddController extends AbstractSimpleFormController {

	private ParamPeriodeManager manager;
	
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		verifieLesDroits();
		return manager.createModeleFeuilleDocumentViewAdd(
			getPeriodeIdFromRequest(request),
			getModeleIdFromRequest(request)
		);
	}
	
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {
		verifieLesDroits();
		ModeleFeuilleDocumentView mfdv = (ModeleFeuilleDocumentView) command;
		manager.saveModeleFeuilleDocumentViewAdd(mfdv);
		return getModelAndViewToPeriode(getPeriodeIdFromRequest(request), getModeleIdFromRequest(request));
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setManager(ParamPeriodeManager manager) {
		this.manager = manager;
	}
}
