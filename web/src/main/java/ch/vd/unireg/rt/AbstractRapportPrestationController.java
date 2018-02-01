package ch.vd.unireg.rt;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import ch.vd.unireg.common.AbstractSimpleFormController;
import ch.vd.unireg.security.SecurityProviderInterface;

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