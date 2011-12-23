package ch.vd.uniregctb.security;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.springframework.web.filter.GenericFilterBean;

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.URLHelper;

/**
 * Ce filtre permet de récupérer de logger les accès aux page web de l'application (voir SIFISC-3085).
 */
public class AccessLogProcessingFilter extends GenericFilterBean {

	private static final Logger GET = Logger.getLogger("web-access.get");
	private static final Logger POST = Logger.getLogger("web-access.post");
	private static final Logger PUT = Logger.getLogger("web-access.put");
	private static final Logger DELETE = Logger.getLogger("web-access.delete");
	private static final Logger OTHER = Logger.getLogger("web-access.other");

	/**
	 * Nombre de requêtes en cours en parallèle
	 */
	private AtomicInteger count = new AtomicInteger(0);

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

		final String visa = AuthenticationHelper.getCurrentPrincipal();

		final long start = System.nanoTime();

		// c'est une nouvelle requête qui arrive
		count.incrementAndGet();

		try {
			filterChain.doFilter(servletRequest, servletResponse);
		}
		finally {
			final long duration = (System.nanoTime() - start) / 1000000;
			final String requestURL = getUrl(servletRequest);
			final String method = getMethod(servletRequest);
			final Logger logger = getLogger(method);
			final int load = count.getAndDecrement();
			logger.info(String.format("[%s] [load=%d] (%d ms) %s", visa, load, duration, requestURL));
		}
	}

	private static Logger getLogger(String method) {
		final Logger logger;
		if ("GET".equals(method)) {
			logger = GET;
		}
		else if ("POST".equals(method)) {
			logger = POST;
		}
		else if ("PUT".equals(method)) {
			logger = PUT;
		}
		else if ("DELETE".equals(method)) {
			logger = DELETE;
		}
		else {
			logger = OTHER;
		}
		return logger;
	}

	private static String getMethod(ServletRequest servletRequest) {
		if (servletRequest instanceof HttpServletRequest) {
			final HttpServletRequest req = (HttpServletRequest) servletRequest;
			return req.getMethod();
		}
		return "n/a";
	}

	private static String getUrl(ServletRequest servletRequest) {
		if (servletRequest instanceof HttpServletRequest) {
			return URLHelper.getTargetUrl((HttpServletRequest) servletRequest);
		}
		else {
			return "n/a";
		}
	}
}
