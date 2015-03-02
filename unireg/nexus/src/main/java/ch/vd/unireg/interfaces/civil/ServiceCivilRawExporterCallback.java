package ch.vd.unireg.interfaces.civil;

import java.util.Map;

import ch.vd.unireg.servlet.remoting.HttpInvokerServiceExporterWithCustomHeaderCallback;
import ch.vd.uniregctb.interfaces.service.ServiceCivilLogger;

public class ServiceCivilRawExporterCallback implements HttpInvokerServiceExporterWithCustomHeaderCallback {

	public static final String INDIVIDU_LOGGING_HEADER = "individu-logging";

	private ServiceCivilLogger serviceCivilLogger;

	public void setServiceCivilLogger(ServiceCivilLogger serviceCivilLogger) {
		this.serviceCivilLogger = serviceCivilLogger;
	}

	@Override
	public void beforeInvocation(Map<String, String> map) {
		if (map != null) {
			final String logging = map.get(INDIVIDU_LOGGING_HEADER);
			serviceCivilLogger.setIndividuLogging(logging != null && logging.equalsIgnoreCase("true"));
		}
	}

	@Override
	public void afterInvocation() {
		serviceCivilLogger.setIndividuLogging(false);
	}
}
