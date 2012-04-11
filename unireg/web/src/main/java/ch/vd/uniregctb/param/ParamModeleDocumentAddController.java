package ch.vd.uniregctb.param;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.param.manager.ParamPeriodeManager;
import ch.vd.uniregctb.param.view.ModeleDocumentView;
import ch.vd.uniregctb.tiers.TiersMapHelper;

import static ch.vd.uniregctb.param.Commun.getModelAndViewToPeriode;
import static ch.vd.uniregctb.param.Commun.getPeriodeIdFromRequest;
import static ch.vd.uniregctb.param.Commun.verifieLesDroits;

public class ParamModeleDocumentAddController extends AbstractSimpleFormController {

	private ParamPeriodeManager manager;
	private TiersMapHelper tiersMapHelper;

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		verifieLesDroits();
		return manager.createModeleDocumentViewAdd(getPeriodeIdFromRequest(request));
	}

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {
		verifieLesDroits();
		ModeleDocumentView mfv = (ModeleDocumentView) command;
		manager.saveModeleDocumentView(mfv);
		return getModelAndViewToPeriode(mfv.getIdPeriode());
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Map referenceData(HttpServletRequest request) throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("typeDocuments", tiersMapHelper.getTypesDeclarationImpotPourParam());
		return data;
	}

	public void setManager(ParamPeriodeManager manager) {
		this.manager = manager;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

}
