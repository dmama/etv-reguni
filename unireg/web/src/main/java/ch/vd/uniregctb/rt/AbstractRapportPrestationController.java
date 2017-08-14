package ch.vd.uniregctb.rt;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.security.SecurityProviderInterface;

public class AbstractRapportPrestationController extends AbstractSimpleFormController {

	public static final String TYPES_ACTIVITE_MAP_NAME = "typesActivite";

	protected SecurityProviderInterface securityProvider;

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Map<String, Object> referenceData(HttpServletRequest request) throws Exception {
		return new HashMap<>();
	}

}