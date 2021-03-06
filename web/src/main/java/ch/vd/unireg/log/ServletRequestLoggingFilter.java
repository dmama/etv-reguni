package ch.vd.unireg.log;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;

import ch.vd.registre.base.date.DateHelper;

/**
 * Cette classe permet d'intercepter les requêtes http et de logger leur contenu dans System.out.
 * <p>
 * Elle est prévue pour fonctionner dans un environnement de développement pour le debugging, elle n'est donc pas prévue pour un déploiement
 * en production.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ServletRequestLoggingFilter implements Filter {

	private FilterConfig filterConfig = null;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;
	}

	@Override
	public void destroy() {
		this.filterConfig = null;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (filterConfig == null) {
			return;
		}

		try {
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			logRequest(httpRequest);
		}
		catch (Exception e) {
			// inutile de spammer pour une erreur de logging
		}

		chain.doFilter(request, response);
	}

	private void logRequest(HttpServletRequest request) {

		final Map<?, ?> map = request.getParameterMap();
		if (map.containsKey("ajax-request")) {
			// on ne log pas les requêtes ajax
			return;
		}

		final String url = request.getRequestURL().toString();
		final String serverName = request.getServerName();
		final String now = new SimpleDateFormat().format(DateHelper.getCurrentDate());
		final String method = request.getMethod();
		final String session = request.getSession().getId();
		final String accept = getAccept(request);

		StringBuilder b = new StringBuilder();
		b.append("\nProcessing ").append(url).append(" (for ").append(serverName).append(" at ").append(now).append(") [").append(method)
				.append("]\n");
		b.append("  Accept: ").append(accept).append('\n');
		b.append("  Session ID: ").append(session).append('\n');
		b.append("  Parameters: ").append(toString(map)).append('\n');

		System.out.println(b.toString());
	}

	private static String getAccept(HttpServletRequest request) {
		final HttpInputMessage inputMessage = new ServletServerHttpRequest(request);
		final List<MediaType> acceptedMediaTypes = inputMessage.getHeaders().getAccept();
		if (acceptedMediaTypes == null || acceptedMediaTypes.isEmpty()) {
			return "";
		}
		final StringBuilder s = new StringBuilder();
		for (MediaType type : acceptedMediaTypes) {
			s.append(type).append(' ');
		}
		return s.toString();
	}

	private String toString(final Object o) {
		if (o == null) {
			return "null";
		}
		if (o instanceof Map<?, ?>) {
			return toString((Map<?, ?>) o);
		}
		else if (o instanceof String[]) {
			return toString((String[]) o);
		}
		else if (o instanceof String) {
			return toString((String) o);
		}
		else {
			return o.toString();
		}
	}

	private String toString(final String s) {
		if (s == null) {
			return "null";
		}
		StringBuilder b = new StringBuilder();
		b.append('\"').append(s).append('\"');
		return b.toString();
	}

	private String toString(final String[] a) {
		if (a == null) {
			return "null";
		}
		if (a.length == 1) {
			return toString(a[0]);
		}
		else {
			StringBuilder b = new StringBuilder();
			b.append('{');
			boolean first = true;
			for (String anA : a) {
				if (!first) {
					b.append(", ");
				}
				b.append(toString(anA));
				first = false;
			}
			b.append('}');
			return b.toString();
		}
	}

	private String toString(final Map<?, ?> map) {
		if (map == null) {
			return "null";
		}
		final StringBuilder b = new StringBuilder();
		b.append('{');
		boolean first = true;
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			if (!first) {
				b.append(", ");
			}
			Object v = entry.getValue();
			b.append(toString(entry.getKey())).append('=').append(toString(v));
			first = false;
		}
		b.append('}');
		return b.toString();
	}

}
