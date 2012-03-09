package ch.vd.uniregctb.param;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.param.manager.ParamPeriodeManager;

public class AbstractParamModeleFeuilleDocumentController extends AbstractSimpleFormController {
	protected ParamPeriodeManager manager;
	public static final String MODELE_FEUILLE_NAME_COMPLETE = "modelesFeuillesForCompletes";
	public static final String MODELE_FEUILLE_NAME_VAUDTAX = "modelesFeuillesForVaudTax";
	public static final String MODELE_FEUILLE_NAME_DEPENSE = "modelesFeuillesForDepense";
	public static final String MODELE_FEUILLE_NAME_HC = "modelesFeuillesForHC";

	protected ParamHelper paramHelper;
	public void setManager(ParamPeriodeManager manager) {
		this.manager = manager;
	}

	@Override
	protected Map<String, Object> referenceData(HttpServletRequest request) throws Exception {
		// TracePoint tp = TracingManager.begin();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put(MODELE_FEUILLE_NAME_COMPLETE,paramHelper.getMapModeleFeuilleForComplete());
		data.put(MODELE_FEUILLE_NAME_VAUDTAX,paramHelper.getMapModeleFeuilleForVaudTax());
		data.put(MODELE_FEUILLE_NAME_DEPENSE,paramHelper.getMapModeleFeuilleForDepense());
		data.put(MODELE_FEUILLE_NAME_HC,paramHelper.getMapModeleFeuilleForHC());
		// TracingManager.end(tp);
		return data;
	}

	public void setParamHelper(ParamHelper paramHelper) {
		this.paramHelper = paramHelper;
	}
}
