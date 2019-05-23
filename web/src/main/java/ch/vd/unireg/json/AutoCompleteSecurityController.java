package ch.vd.unireg.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.unireg.common.StringComparator;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.security.Operateur;

/**
 * Contrôleur qui expose des données du service sécurité dans un format Json (utilisé ensuite dans le mécanisme d'autocompletion).
 */
@Controller
public class AutoCompleteSecurityController {

	//private static final Logger LOGGER = LoggerFactory.getLogger(AutoCompleteSecurityController.class);

	private ServiceSecuriteService serviceSecuriteService;

	private enum Category {
		USER
	}

	/**
	 * Retourne des données du service de sécurité sous forme JSON (voir http://blog.springsource.com/2010/01/25/ajax-simplifications-in-spring-3-0/)
	 *
	 * @param category le catégorie de données désirée
	 * @param term     un critère de recherche des données
	 * @return le nombre d'immeubles du contribuable spécifié.
	 * @throws Exception en cas de problème
	 */
	@RequestMapping(value = "/autocomplete/security.do", method = RequestMethod.GET)
	@ResponseBody
	public List<AutoCompleteItem> security(@RequestParam("category") String category, @RequestParam("term") String term) throws Exception {

		final Set<Category> categories = parseCategories(category);

		// les urls sont envoyées en UTF-8 par jQuery mais interprétées en ISO-8859-1 par Tomcat
		// -> depuis tomcat 8, ce n'est plus le cas, l'interprétation se fait bien en UTF-8 par défaut (http://tomcat.apache.org/migration-8.html#URIEncoding)
		//term = EncodingFixHelper.fixFromIso(term);

		// on ignore les accents
		term = StringComparator.toLowerCaseWithoutAccent(term);

		final List<AutoCompleteItem> list = new ArrayList<>();

		if (categories.contains(Category.USER)) {
			final List<TypeCollectivite> colls = Arrays.asList(TypeCollectivite.SIGLE_ACI, TypeCollectivite.SIGLE_ACIA, TypeCollectivite.SIGLE_ACIFD,
					TypeCollectivite.SIGLE_ACIPP, TypeCollectivite.SIGLE_CIR,TypeCollectivite.SIGLE_S_ACI);
			final List<Operateur> operateurs = serviceSecuriteService.getUtilisateurs(colls);
			for (Operateur operateur : operateurs) {
				if (operateur.getCode().toLowerCase().startsWith(term) || StringComparator.toLowerCaseWithoutAccent(operateur.getNom()).startsWith(term) ||
						StringComparator.toLowerCaseWithoutAccent(operateur.getPrenom()).startsWith(term)) {
					final String label = operateur.getNom() + ' ' + operateur.getPrenom();
					list.add(new AutoCompleteItem(label, label + " (" + operateur.getCode() + ')', operateur.getCode()));
				}
				if (list.size() >= 50) { // [SIFISC-482] on limite à 50 le nombre de résultats retournés
					break;
				}
			}
		}

		return list;
	}

	private static Set<Category> parseCategories(String category) {
		final Set<Category> categories = EnumSet.noneOf(Category.class);
		if ("user".equalsIgnoreCase(category)) {
			categories.add(Category.USER);
		}
		return categories;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceSecuriteService(ServiceSecuriteService serviceSecuriteService) {
		this.serviceSecuriteService = serviceSecuriteService;
	}
}