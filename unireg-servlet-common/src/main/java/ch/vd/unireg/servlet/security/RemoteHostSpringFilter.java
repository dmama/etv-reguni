package ch.vd.unireg.servlet.security;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.web.filter.GenericFilterBean;

/**
 * Filtre qui permet de refuser une requête en fonction de l'adresses IP du client qui appelle.
 * <p/>
 * Cette classe est basée sur la classe homonyme de shared-iam, avec des améliorations sur le format des adresses IP (wildcards à la place de regexp) + perfs.
 */
public class RemoteHostSpringFilter extends GenericFilterBean {

	private final static Logger LOGGER = Logger.getLogger(RemoteHostSpringFilter.class);

	private Pattern[] allowed = null;
	private Pattern[] denied = null;

	public void setAllowed(String allowed) {
		this.allowed = parseIPAddresses(allowed);
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setDenied(String denied) {
		this.denied = parseIPAddresses(denied);
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		final String clientAddr = request.getRemoteAddr();

		if (isInvalidAddress(clientAddr)) {
			handleInvalidAccess(request, response, clientAddr);
			return;
		}

		chain.doFilter(request, response);
	}

	protected boolean isInvalidAddress(String address) {
		return hasMatch(address, denied) || (allowed.length > 0) && !hasMatch(address, allowed);
	}

	private void handleInvalidAccess(ServletRequest request, ServletResponse response, String clientAddr) throws IOException {
		final String url = ((HttpServletRequest) request).getRequestURL().toString();
		LOGGER.warn("Invalid access attempt to " + url + " from " + clientAddr);
		((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
	}

	protected static boolean hasMatch(String clientAddr, Pattern[] patterns) {
		//noinspection ForLoopReplaceableByForEach
		for (int i = 0, length = patterns.length; i < length; i++) {
			final Pattern p = patterns[i];
			if (p.matcher(clientAddr).matches()) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void destroy() {
		this.allowed = null;
		this.denied = null;
	}

	private static Pattern[] parseIPAddresses(String initParam) {
		if (StringUtils.isBlank(initParam)) {
			return new Pattern[0];
		}
		else {
			final String[] splitted = initParam.split(",");
			final Pattern patterns[] = new Pattern[splitted.length];
			for (int i = 0, length = splitted.length; i < length; i++) {
				patterns[i] = Pattern.compile(wildcardToRegExp(splitted[i]));
			}
			return patterns;
		}
	}

	/**
	 * Cette méthode permet de convertir des adresses ip au format wildcard ("10.240.6.*") au format regexp ("10\.240\.6\.[.0-9]+")
	 *
	 * @param wildcard une adresse IP qui peut contenir des wildcards
	 * @return l'adresse IP convertie au format regexp
	 */
	protected static String wildcardToRegExp(String wildcard) {
		return wildcard.replaceAll("\\.", "\\\\.").replaceAll("\\*", "[.0-9]+");
	}
}