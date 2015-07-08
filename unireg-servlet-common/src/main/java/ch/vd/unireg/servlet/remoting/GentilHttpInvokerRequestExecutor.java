package ch.vd.unireg.servlet.remoting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.springframework.remoting.httpinvoker.CommonsHttpInvokerRequestExecutor;
import org.springframework.remoting.httpinvoker.HttpInvokerClientConfiguration;
import org.springframework.remoting.support.RemoteInvocationResult;

public class GentilHttpInvokerRequestExecutor extends CommonsHttpInvokerRequestExecutor {

	private final String serviceName;

	private static final Logger LOGGER = Logger.getLogger(GentilHttpInvokerRequestExecutor.class);

	private static final long ONE_MINUTE = TimeUnit.MINUTES.toMillis(1);

	public GentilHttpInvokerRequestExecutor(String serviceName) {
		this.serviceName = serviceName;
	}

	private static class ServiceNotAvailableException extends IOException {
		private ServiceNotAvailableException(String message) {
			super(message);
		}
	}

	@Override
	protected RemoteInvocationResult doExecuteRequest(HttpInvokerClientConfiguration config, ByteArrayOutputStream baos) throws IOException, ClassNotFoundException {
		while (true) {
			try {
				return super.doExecuteRequest(config, baos);
			}
			catch (ServiceNotAvailableException e) {
	            LOGGER.warn("Service " + serviceName + " currently not responding (" + e.getMessage() + ")... Waiting for one minute...");
				try {
					Thread.sleep(ONE_MINUTE);
				}
				catch (InterruptedException ie) {
					LOGGER.error("Waiting for service " + serviceName + " availability interrupted", ie);
					throw new RuntimeException(ie);
				}
			}
		}
	}

	@Override
	protected void executePostMethod(HttpInvokerClientConfiguration config, HttpClient httpClient, PostMethod postMethod) throws IOException {
		try {
			super.executePostMethod(config, httpClient, postMethod);
		}
		catch (ConnectException e) {
			throw new ServiceNotAvailableException(e.getMessage());
		}
	}

	@Override
	protected void validateResponse(HttpInvokerClientConfiguration config, PostMethod postMethod) throws IOException {
		if (postMethod.getStatusCode() == 503) {
			// service temporary unavailable...
			throw new ServiceNotAvailableException(String.format("%d %s", postMethod.getStatusCode(), postMethod.getStatusText()));
		}
		else {
			super.validateResponse(config, postMethod);
		}
	}
}
