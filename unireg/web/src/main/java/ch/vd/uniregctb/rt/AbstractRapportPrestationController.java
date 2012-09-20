package ch.vd.uniregctb.rt;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.tiers.TiersMapHelper;

public class AbstractRapportPrestationController extends AbstractSimpleFormController {

	public static final String TYPES_ACTIVITE_MAP_NAME = "typesActivite";

	private TiersMapHelper tiersMapHelper;

	public TiersMapHelper getTiersMapHelper() {
		return tiersMapHelper;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}
	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Map<String, Object> referenceData(HttpServletRequest request) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();
		data.put(TYPES_ACTIVITE_MAP_NAME, tiersMapHelper.getMapTypeActivite());
		return data;
	}

}