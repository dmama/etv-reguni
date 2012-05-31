package ch.vd.unireg.servlet.remoting;

import java.util.Map;

public interface HttpInvokerServiceExporterWithCustomHeaderCallback {

	/**
	 * Cette méthode est appelée avant chaque invocation de méthode du service exporté.
	 *
	 * @param customHeaders les headers personnalisés trouvés sur la requête http
	 */
	void beforeInvocation(Map<String, String> customHeaders);

	/**
	 * Cette méthode est appelée après chaque invocation de méthode du service exporté.
	 */
	void afterInvocation();
}
