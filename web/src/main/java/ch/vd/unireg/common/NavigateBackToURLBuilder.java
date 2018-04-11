package ch.vd.unireg.common;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Construit l'URL pour retourner à une page précédemment consultée par l'utilisateur. Cette méthode permet de simuler l'utilisation
 * répétée du bouton <i>back</i> en remontant sélectivement dans l'historique des pages consultées par l'utilisateur.
 * Si la page spécifiée n'existe pas dans l'historique de navigation, l'utilisateur est renvoyé vers la page en utilisant des paramètres par défaut.
 */
public class NavigateBackToURLBuilder {

	private final StringBuilder url = new StringBuilder("redirect:/navigation/backTo.do?");

	/**
	 * Construit un builder pour retourner à la page spécifiée précédemment consultée par l'utilisateur
	 *
	 * @param pageUrl l'URL de la page sur laquelle on veut revenir (e.g. '/tiers/visu.do')
	 */
	public NavigateBackToURLBuilder(@NotNull String pageUrl) {
		url.append("pageUrl=").append(URLHelper.encodeQueryString(pageUrl));
	}

	/**
	 * Ajoute une page de destination alternative à la page principale. Le plus récente des pages spécifiées sera choisie.
	 *
	 * @param pageUrl l'URL d'une page page alternative sur laquelle on veut revenir (e.g. '/tiers/visu.do')
	 */
	public NavigateBackToURLBuilder or(@NotNull String pageUrl) {
		url.append("&pageUrl=").append(URLHelper.encodeQueryString(pageUrl));
		return this;
	}

	/**
	 * Ajoute la page par défaut si la page de retour demandée n'existe pas dans l'historique de navigation.
	 *
	 * @param defaultPageUrl l'URL de la page par défaut (e.g. '/tiers/visu.do')
	 * @param defaultParams  les paramètres par défaut de la page (e.g. 'id=12345')
	 * @return l'URL finale
	 */
	public String defaultTo(@NotNull String defaultPageUrl, @Nullable String defaultParams) {
		url.append("&defaultPageUrl=").append(URLHelper.encodeQueryString(defaultPageUrl));
		if (StringUtils.isNotBlank(defaultParams)) {
			url.append("&defaultParams=").append(URLHelper.encodeQueryString(defaultParams));
		}
		return url.toString();
	}
}

