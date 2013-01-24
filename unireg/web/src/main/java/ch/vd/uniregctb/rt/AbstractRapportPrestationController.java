package ch.vd.uniregctb.rt;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.TiersMapHelper;

public class AbstractRapportPrestationController extends AbstractSimpleFormController {

	public static final String TYPES_ACTIVITE_MAP_NAME = "typesActivite";

	private TiersMapHelper tiersMapHelper;
	protected SecurityProviderInterface securityProvider;

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Map<String, Object> referenceData(HttpServletRequest request) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();
		return data;
	}

}