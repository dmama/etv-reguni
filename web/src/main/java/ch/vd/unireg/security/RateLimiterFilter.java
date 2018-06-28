package ch.vd.unireg.security;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.web.filter.GenericFilterBean;

import ch.vd.unireg.common.AuthenticationHelper;

/**
 * [SIFISC-29260] Filtre qui s'assure que le nombre de requêtes simultanées pour chaque utilisateur ne dépasse pas une valeur limite. Si cette valeur limite est dépassée, un réponse HTTP 503 (server temporarily overloaded) est retounée.
 */
@ManagedResource
public class RateLimiterFilter extends GenericFilterBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(RateLimiterFilter.class);

	/**
	 * le nombre maximal de requêtes simultanées pour chaque utilisateur.
	 */
	private boolean enabled;

	/**
	 * <i>vrai</i> si le filtre est activé; <i>faux</i> s'il ne fait rien.
	 */
	private int maxLoadPerUser;

	private final Map<String, AtomicInteger> loadPerUser = new ConcurrentHashMap<>();

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		if (!enabled) {
			// pas activé : on ne fait rien
			chain.doFilter(request, response);
			return;
		}

		final String user = AuthenticationHelper.getCurrentPrincipal();
		if (user == null) {
			throw new IllegalArgumentException("L'utilisateur n'est pas défini");
		}

		final AtomicInteger loadForUser = loadPerUser.computeIfAbsent(user, k -> new AtomicInteger(0));

		final int currentLoad = loadForUser.incrementAndGet();
		if (currentLoad > maxLoadPerUser) {
			// charge maximale atteinte -> on retourne une erreur HTTP 503
			LOGGER.warn("Load maximal atteint pour l'utilisateur " + user + " (load=" + currentLoad + ", max=" + maxLoadPerUser + ")");
			((HttpServletResponse) response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			loadForUser.decrementAndGet();
			return;
		}

		// tout va bien
		try {
			chain.doFilter(request, response);
		}
		finally {
			loadForUser.decrementAndGet();
		}
	}

	@ManagedAttribute(description = "l'état d'activation du filtre")
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@ManagedAttribute(description = "nombre maximal de requêtes simultanées pour chaque utilisateur")
	public int getMaxLoadPerUser() {
		return maxLoadPerUser;
	}

	@ManagedAttribute(description = "nombre maximal de requêtes simultanées pour chaque utilisateur")
	public void setMaxLoadPerUser(int maxLoadPerUser) {
		this.maxLoadPerUser = maxLoadPerUser;
	}

	@ManagedAttribute(description = "load combiné de tous les utilisateur")
	public int getTotalLoad() {
		return loadPerUser.values().stream()
				.mapToInt(AtomicInteger::get)
				.reduce((left, right) -> left + right)
				.orElse(0);
	}

	@ManagedAttribute(description = "load pour chaque utilisateur")
	public List<String> getLoadDetails() {
		return loadPerUser.entrySet().stream()
				.map(e -> e.getKey() + "=" + e.getValue().get())
				.collect(Collectors.toList());
	}

	@ManagedOperation(description = "remet à zéro tous les loads")
	public void reset() {
		loadPerUser.clear();
	}

	@ManagedOperation(description = "active le filtre")
	public void enable() {
		enabled = true;
	}

	@ManagedOperation(description = "désactive le filtre")
	public void disable() {
		enabled = false;
	}
}
