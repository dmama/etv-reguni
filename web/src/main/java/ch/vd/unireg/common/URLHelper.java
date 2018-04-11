package ch.vd.unireg.common;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

	/**
	 * Translates a string into application/x-www-form-urlencoded format using UTF-8. L'exception {@link UnsupportedEncodingException} est wrappée dans une {@link RuntimeException}.
	 *
	 * @param queryString la query string
	 * @return la query string encodée
	 */
	public static String encodeQueryString(@NotNull String queryString) {
		try {
			return URLEncoder.encode(queryString, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Construit l'URL pour retourner à la page précédante. Cette méthode permet de simuler l'utilisation
	 * du bouton <i>back</i> tout en assurant d'atterrir sur une page valide si l'historique est vide.
	 *
	 * @param defaultPageUrl       l'URL de la page sur laquelle on veut revenir (e.g. '/tiers/visu.do')
	 * @param defaultParams les paramètres par défaut à utiliser si la page n'est pas trouvée dans l'historique (e.g. 'id=12345')
	 */
	public static String navigateBack(@NotNull String defaultPageUrl, @Nullable String defaultParams) {
		String url = "redirect:/navigation/back.do?defaultPageUrl=" + URLHelper.encodeQueryString(defaultPageUrl);
		if (StringUtils.isNotBlank(defaultParams)) {
			url = url + "&defaultParams=" + URLHelper.encodeQueryString(defaultParams);
		}
		return url;
	}

	/**
	 * Débute la construction d'une URL pour retourner à une page précédemment consultée par l'utilisateur. Cette méthode permet de simuler l'utilisation
	 * répétée du bouton <i>back</i> en remontant sélectivement dans l'historique des pages consultées par l'utilisateur,
	 * tout en assurant d'atterrir sur une page valide si la page souhaitée ne se trouve pas dans l'historique.
	 *
	 * @param pageUrl l'URL de la page sur laquelle on veut revenir (e.g. '/tiers/visu.do')
	 */
	public static NavigateBackToURLBuilder navigateBackTo(@NotNull String pageUrl) {
		return new NavigateBackToURLBuilder(pageUrl);
	}
}
