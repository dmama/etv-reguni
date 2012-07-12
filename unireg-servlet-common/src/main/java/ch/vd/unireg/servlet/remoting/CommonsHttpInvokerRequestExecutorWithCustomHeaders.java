package ch.vd.unireg.servlet.remoting;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.methods.PostMethod;
import org.springframework.remoting.httpinvoker.CommonsHttpInvokerRequestExecutor;
import org.springframework.remoting.httpinvoker.HttpInvokerClientConfiguration;

/**
 * Cette classe spécialise le request invoker pour ajouter des headers personnalisés lors des appels http en spring remoting.
 *
 * @see {@link HttpInvokerServiceExporterWithCustomHeaders}
 */
public class CommonsHttpInvokerRequestExecutorWithCustomHeaders extends CommonsHttpInvokerRequestExecutor {

	private String prefix = "unireg-";

	private final ThreadLocal<Map<String, String>> customHeaders = new ThreadLocal<Map<String, String>>() {
		@Override
		protected Map<String, String> initialValue() {
			return new HashMap<String, String>();
		}
	};

	@Override
	protected PostMethod createPostMethod(HttpInvokerClientConfiguration config) throws IOException {
		final PostMethod postMethod = super.createPostMethod(config);
		for (Map.Entry<String, String> entry : customHeaders.get().entrySet()) {
			postMethod.addRequestHeader(String.format("%s%s", prefix, entry.getKey()), entry.getValue());
		}
		return postMethod;
	}

	/**
	 * Défini un nouveau header personnalisé <b>pour le thread courant</b>.
	 *
	 * @param key   la clé du header
	 * @param value la valeur du header
	 */
	public void setCustomHeader(String key, String value) {
		customHeaders.get().put(key, value);
	}

	/**
	 * Supprime un header personnlaisé <b>pour le thread courant</b>.
	 *
	 * @param key la clé du header
	 * @return la valeur supprimé
	 */
	public String removeCustomHeader(String key) {
		return customHeaders.get().remove(key);
	}

	public void setCustomHeadersPrefix(String prefix) {
		this.prefix = prefix;
	}
}
