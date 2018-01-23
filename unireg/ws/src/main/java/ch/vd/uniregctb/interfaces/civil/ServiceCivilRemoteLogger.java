package ch.vd.uniregctb.interfaces.civil;

import ch.vd.unireg.servlet.remoting.UniregHttpInvokerRequestExecutorWithCustomHeaders;
import ch.vd.uniregctb.interfaces.service.ServiceCivilLogger;

/**
 * @see ServiceCivilRemoteLogger la même classe dans le projet /web
 */
public class ServiceCivilRemoteLogger implements ServiceCivilLogger {

	public static final String INDIVIDU_LOGGING_HEADER = "individu-logging";

	private UniregHttpInvokerRequestExecutorWithCustomHeaders executor;

	public void setExecutor(UniregHttpInvokerRequestExecutorWithCustomHeaders executor) {
		this.executor = executor;
	}

	@Override
	public void setIndividuLogging(boolean value) {
		if (value) {
			executor.setCustomHeader(INDIVIDU_LOGGING_HEADER, String.valueOf(true));
		}
		else {
			executor.removeCustomHeader(INDIVIDU_LOGGING_HEADER);
		}
	}
}
