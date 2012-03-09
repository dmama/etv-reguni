package ch.vd.uniregctb.common;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

public abstract class URLHelper {
	
	/**
	 * Retourne l'URL de destination complète (avec les paramètres) de la requête spécifiée.
	 *
	 * @param req une requête http
	 * @return l'URL de destination
	 */
	public static String getTargetUrl(HttpServletRequest req) {
		final String sp = req.getServletPath(); // e.g. /tiers/list.do
		final String url = StringUtils.isBlank(sp) ? req.getRequestURL().toString() : sp;
		final String queryString = req.getQueryString();
		if (queryString == null) {
			return url;
		}
		else {
			return String.format("%s?%s", url, queryString);
		}
	}
}
