package ch.vd.unireg;

import java.io.IOException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping(value = "/navigation")
@Controller
public class NavigationController {

	/**
	 * Retourne à la page précédante. Cette méthode permet de simuler l'utilisation du bouton <i>back</i> tout en assurant
	 * d'atterrir sur une page valide si l'historique est vide.
	 *
	 * @param defaultPageUrl       l'URL de la page sur laquelle on veut revenir (e.g. '/tiers/visu.do')
	 * @param defaultParams les paramètres par défaut à utiliser si la page n'est pas trouvée dans l'historique (e.g. 'id=12345')
	 */
	@RequestMapping(value = "/back.do", method = RequestMethod.GET)
	public String back(@RequestParam String defaultPageUrl, @RequestParam(required = false) String defaultParams, Model model) throws IOException {
		model.addAttribute("defaultPageUrl", defaultPageUrl);
		model.addAttribute("defaultParams", defaultParams);
		return "navigation/back";
	}

	/**
	 * Retourne à une page précédemment consultée par l'utilisateur. Cette méthode permet de simuler l'utilisation répétée
	 * du bouton <i>back</i> en remontant sélectivement dans l'historique des pages consultées par l'utilisateur,
	 * tout en assurant d'atterrir sur une page valide si la page souhaitée ne se trouve pas dans l'historique.
	 *
	 * @param pageUrl       l'URL de la page sur laquelle on veut revenir (e.g. '/tiers/visu.do')
	 * @param defaultParams les paramètres par défaut à utiliser si la page n'est pas trouvée dans l'historique (e.g. 'id=12345')
	 */
	@RequestMapping(value = "/backTo.do", method = RequestMethod.GET)
	public String backTo(@RequestParam String pageUrl, @RequestParam(required = false) String defaultParams, Model model) throws IOException {
		model.addAttribute("pageUrl", pageUrl);
		model.addAttribute("defaultParams", defaultParams);
		return "navigation/backTo";
	}
}
