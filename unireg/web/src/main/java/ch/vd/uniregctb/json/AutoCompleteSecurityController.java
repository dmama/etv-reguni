package ch.vd.uniregctb.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.securite.model.Operateur;
import ch.vd.uniregctb.common.StringComparator;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;

/**
 * Contrôleur qui expose des données du service sécurité dans un format Json (utilisé ensuite dans le mécanisme d'autocompletion).
 */
@Controller
public class AutoCompleteSecurityController {

	protected final Logger LOGGER = Logger.getLogger(AutoCompleteSecurityController.class);

	private ServiceSecuriteService serviceSecuriteService;

	private enum Category {
		USER
	}

	@SuppressWarnings({"UnusedDeclaration"})
	private static class Item {
		/**
		 * Chaîne de caractères utilisée dans le champ d'autocompletion
		 */
		private final String label;
		/**
		 * Chaîne de caractères utilisée dans la liste (dropdown) des valeurs disponibles
		 */
		private final String desc;
		/**
		 * Identifiant optionnel pouvant être affecté à un autre champ (généralement caché).
		 */
		private String id1;
		/**
		 * Second identifiant optionnel pouvant être affecté à un autre champ (généralement caché).
		 */
		private String id2;

		private Item(String label, String desc, String id1, String id2) {
			this.label = label;
			this.desc = desc;
			this.id1 = id1;
			this.id2 = id2;
		}

		public String getLabel() {
			return label;
		}

		public String getDesc() {
			return desc;
		}

		public String getId1() {
			return id1;
		}

		public String getId2() {
			return id2;
		}
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
	public List<Item> security(@RequestParam("category") String category, @RequestParam("term") String term) throws Exception {

		final Set<Category> categories = parseCategories(category);

		// les urls sont envoyées en UTF-8 par jQuery mais interprétées en ISO-8859-1 par Tomcat
		final byte[] bytes = term.getBytes("ISO-8859-1");
		term = new String(bytes, "UTF-8");

		// on ignore les accents
		term = StringComparator.toLowerCaseWithoutAccent(term);

		final List<Item> list = new ArrayList<Item>();

		if (categories.contains(Category.USER)) {
			final List<EnumTypeCollectivite> colls = Arrays.asList(EnumTypeCollectivite.SIGLE_ACI, EnumTypeCollectivite.SIGLE_ACIA, EnumTypeCollectivite.SIGLE_ACIFD,
					EnumTypeCollectivite.SIGLE_ACIPP, EnumTypeCollectivite.SIGLE_CIR, EnumTypeCollectivite.SIGLE_S_ACI);
			final List<Operateur> operateurs = serviceSecuriteService.getUtilisateurs(colls);
			if (operateurs != null) {
				for (Operateur operateur : operateurs) {
					if (operateur.getCode().toLowerCase().startsWith(term) || StringComparator.toLowerCaseWithoutAccent(operateur.getNom()).startsWith(term) ||
							StringComparator.toLowerCaseWithoutAccent(operateur.getPrenom()).startsWith(term)) {
						final String label = operateur.getNom() + ' ' + operateur.getPrenom();
						list.add(new Item(label, label + " (" + operateur.getCode() + ')', operateur.getCode(), String.valueOf(operateur.getIndividuNoTechnique())));
					}
					if (list.size() >= 50) { // [SIFISC-482] on limite à 50 le nombre de résultats retournés
						break;
					}
				}
			}
		}

		return list;
	}

	private static Set<Category> parseCategories(String category) {
		final Set<Category> categories = new HashSet<Category>();
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