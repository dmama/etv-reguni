package ch.vd.uniregctb.servlet;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.GenericFilterBean;

public class LastChanceExceptionLoggingFilter extends GenericFilterBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(LastChanceExceptionLoggingFilter.class);

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		try {
			chain.doFilter(request, response);
		}
		catch (IOException | ServletException | RuntimeException | Error e) {
			final Throwable root = deepDiveIntoServletException(e);
			final String requestDescription = buildRequestDescription(request);
			LOGGER.error(String.format("Exception envoyée et non-trappée par le traitement de la requête %s", requestDescription), root);
			throw e;
		}
	}

	private static Throwable deepDiveIntoServletException(Throwable e) {
		Throwable t = e;
		while (t instanceof ServletException && t.getCause() != null) {
			t = t.getCause();
		}
		return t;
	}

	private static String buildRequestDescription(ServletRequest request) {
		if (request instanceof HttpServletRequest) {
			final String uri = getLocalUri((HttpServletRequest) request);
			final Map<String, String[]> params = request.getParameterMap();
			if (params != null && !params.isEmpty()) {
				final StringBuilder b = new StringBuilder();
				b.append(uri);
				b.append('?');
				boolean first = true;
				for (Map.Entry<String, String[]> entry : params.entrySet()) {
					for (String value : entry.getValue()) {
						if (!first) {
							b.append('&');
						}
						b.append(entry.getKey()).append('=').append(value);
						first = false;
					}
				}
				return b.toString();
			}
			else {
				return uri;
			}
		}
		else {
			return request.getServletContext().getContextPath();
		}
	}

	private static String getLocalUri(HttpServletRequest request) {
		final String fullUri = request.getRequestURI();
		final String contextPath = request.getContextPath();
		if (fullUri != null && contextPath != null && fullUri.startsWith(contextPath)) {
			return fullUri.substring(contextPath.length());
		}
		else {
			return fullUri;
		}
	}
}
