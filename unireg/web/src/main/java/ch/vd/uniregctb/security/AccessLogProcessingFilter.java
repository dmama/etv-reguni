package ch.vd.uniregctb.security;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.springframework.web.filter.GenericFilterBean;

import ch.vd.uniregctb.common.AuthenticationHelper;

/**
 * Ce filtre permet de récupérer de logger les accès aux page web de l'application (voir SIFISC-3085).
 */
public class AccessLogProcessingFilter extends GenericFilterBean {

	private static final Logger GET = Logger.getLogger("web-access.GET");
	private static final Logger POST = Logger.getLogger("web-access.POST");
	private static final Logger PUT = Logger.getLogger("web-access.PUT");
	private static final Logger DELETE = Logger.getLogger("web-access.DELETE");
	private static final Logger OTHER = Logger.getLogger("web-access.OTHER");

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

		final String visa = AuthenticationHelper.getCurrentPrincipal();

		final long start = System.nanoTime();
		try {
			filterChain.doFilter(servletRequest, servletResponse);
		}
		finally {
			final long duration = (System.nanoTime() - start) / 1000000;
			final String requestURL = getUrl(servletRequest);
			final String method = getMethod(servletRequest);
			final Logger logger = getLogger(method);
			logger.info(String.format("[%s] (%d ms) %s", visa, duration, requestURL));
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
			final HttpServletRequest req = (HttpServletRequest) servletRequest;

			final String url = req.getRequestURL().toString();
			final String queryString = req.getQueryString();
			if (queryString == null) {
				return url;
			}
			else {
				return url + "?" + queryString;
			}
		}
		else {
			return "n/a";
		}
	}
}
