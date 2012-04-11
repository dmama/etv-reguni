package ch.vd.uniregctb.security;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.web.filter.GenericFilterBean;

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.URLHelper;
import ch.vd.uniregctb.load.DetailedLoadMeter;
import ch.vd.uniregctb.load.DetailedLoadMonitorable;
import ch.vd.uniregctb.load.LoadDetail;
import ch.vd.uniregctb.load.LoadDetailRenderer;

/**
 * Ce filtre permet de récupérer de logger les accès aux page web de l'application (voir SIFISC-3085).
 */
public class AccessLogProcessingFilter extends GenericFilterBean implements DetailedLoadMonitorable {

	private static final Logger GET = Logger.getLogger("web-access.get");
	private static final Logger POST = Logger.getLogger("web-access.post");
	private static final Logger PUT = Logger.getLogger("web-access.put");
	private static final Logger DELETE = Logger.getLogger("web-access.delete");
	private static final Logger OTHER = Logger.getLogger("web-access.other");

	/**
	 * Les requêtes doivent être affichées par leur URL
	 */
	private static final LoadDetailRenderer<ServletRequest> RENDERER = new LoadDetailRenderer<ServletRequest>() {
		@Override
		public String toString(ServletRequest object) {
			return getUrl(object);
		}
	};

	/**
	 * Nombre de requêtes en cours en parallèle
	 */
	private final DetailedLoadMeter<ServletRequest> loadMeter = new DetailedLoadMeter<ServletRequest>(RENDERER);

	@Override
	public int getLoad() {
		return loadMeter.getLoad();
	}

	@Override
	public List<LoadDetail> getLoadDetails() {
		return loadMeter.getLoadDetails();
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

		final String visa = AuthenticationHelper.getCurrentPrincipal();

		// c'est une nouvelle requête qui arrive
		final long start = loadMeter.start(servletRequest);

		try {
			filterChain.doFilter(servletRequest, servletResponse);
		}
		finally {
			final long end = loadMeter.end();
			final String requestURL = getUrl(servletRequest);
			final String method = getMethod(servletRequest);
			final Logger logger = getLogger(method);
			final long duration = TimeUnit.NANOSECONDS.toMillis(end - start);
			final int load = loadMeter.getLoad() + 1;       // +1 car le decrement vient d'être fait dans l'appel à loadMeter.end()
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
