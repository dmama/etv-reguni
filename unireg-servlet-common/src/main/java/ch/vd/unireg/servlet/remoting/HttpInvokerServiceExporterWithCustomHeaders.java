package ch.vd.unireg.servlet.remoting;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

/**
 * Ce service exporter décode d'éventuels headers personnalisés transmis avec la requête http, et les transmettre à un callback avant et après l'invocation de la méthode du service.
 *
 * @see {@link CommonsHttpInvokerRequestExecutorWithCustomHeaders}
 */
public class HttpInvokerServiceExporterWithCustomHeaders extends HttpInvokerServiceExporter {

	private final static Logger LOGGER = Logger.getLogger(HttpInvokerServiceExporterWithCustomHeaders.class);

	private String prefix = "unireg-";
	private HttpInvokerServiceExporterWithCustomHeaderCallback callback;

	@Override
	protected RemoteInvocation readRemoteInvocation(HttpServletRequest request) throws IOException, ClassNotFoundException {

		final Map<String, String> customHeaders = new HashMap<String, String>();

		final Enumeration enumerations = request.getHeaderNames();
		while (enumerations.hasMoreElements()) {
			final String key = (String) enumerations.nextElement();
			if (key.startsWith(prefix)) {
				final String value = request.getHeader(key);
				customHeaders.put(key.substring(prefix.length()), value);
			}
		}

		if (callback != null) {
			try {
				callback.beforeInvocation(customHeaders);
			}
			catch (Exception e) {
				LOGGER.warn("Callback invocation failed: " + e.getMessage(), e);
			}
		}

		return super.readRemoteInvocation(request);
	}

	@Override
	protected void writeRemoteInvocationResult(HttpServletRequest request, HttpServletResponse response, RemoteInvocationResult result) throws IOException {

		if (callback != null) {
			try {
				callback.afterInvocation();
			}
			catch (Exception e) {
				LOGGER.warn("Callback invocation failed: " + e.getMessage(), e);
			}
		}

		super.writeRemoteInvocationResult(request, response, result);
	}

	public void setCustomHeadersPrefix(String prefix) {
		this.prefix = prefix;
	}

	public void setCallback(HttpInvokerServiceExporterWithCustomHeaderCallback callback) {
		this.callback = callback;
	}
}
