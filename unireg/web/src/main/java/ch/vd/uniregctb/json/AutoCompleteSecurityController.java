package ch.vd.uniregctb.json;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.securite.model.Operateur;
import ch.vd.uniregctb.common.StringComparator;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;

/**
 * Contrôleur qui expose des données du service sécurité dans un format Json (utilisé ensuite dans le mécanisme d'autocompletion).
 */
public class AutoCompleteSecurityController extends JsonController {

	protected final Logger LOGGER = Logger.getLogger(AutoCompleteSecurityController.class);

	private ServiceSecuriteService serviceSecuriteService;

	private enum Category {
		USER
	}

	private static class Item {
		/**
		 * Chaîne de caractères utilisée dans le champ d'autocompletion
		 */
		private String label;
		/**
		 * Chaîne de caractères utilisée dans la liste (dropdown) des valeurs disponibles
		 */
		private String desc;
		/**
		 * Identifiant optionnel pouvant être affecté à un autre champ (généralement caché).
		 */
		private String id1;
		/**
		 * Second identifiant optionnel pouvant être affecté à un autre champ (généralement caché).
		 */
		private String id2;

		private Item(String label, String desc) {
			this.label = label;
			this.desc = desc;
		}

		private Item(String label, String desc, String id1) {
			this.label = label;
			this.desc = desc;
			this.id1 = id1;
		}

		private Item(String label, String desc, String id1, String id2) {
			this.label = label;
			this.desc = desc;
			this.id1 = id1;
			this.id2 = id2;
		}

		public String toJson() {
			StringBuilder sb = new StringBuilder();
			sb.append("{\"label\":\"").append(label).append("\",");
			sb.append("\"desc\":\"").append(desc).append("\",");
			sb.append("\"id1\":\"").append(id1).append("\",");
			sb.append("\"id2\":\"").append(id2).append("\"}");
			return sb.toString();
		}
	}

	private static class ListItem extends ArrayList<Item> {
		public String toJson() {
			StringBuilder sb = new StringBuilder();
			sb.append('[');
			for (int i = 0, thisSize = this.size(); i < thisSize; i++) {
				final Item item = this.get(i);
				sb.append(item.toJson());
				if (i < thisSize - 1) {
					sb.append(',');
				}
			}
			sb.append(']');
			return sb.toString();
		}
	}

	@Override
	protected String buildJsonResponse(HttpServletRequest request) throws Exception {
		final String category = request.getParameter("category");
		final Set<Category> categories = parseCategories(category);

		// les urls sont envoyées en UTF-8 par jQuery mais interprétées en ISO-8859-1 par Tomcat
		String term = request.getParameter("term");
		final byte[] bytes = term.getBytes("ISO-8859-1");
		term = new String(bytes, "UTF-8");

		// on ignore les accents
		term = StringComparator.toLowerCaseWithoutAccent(term);

		final ListItem list = new ListItem();

		if (categories.contains(Category.USER)) {
			final List<EnumTypeCollectivite> colls = Arrays.asList(EnumTypeCollectivite.SIGLE_ACI, EnumTypeCollectivite.SIGLE_ACIA, EnumTypeCollectivite.SIGLE_ACIFD,
					EnumTypeCollectivite.SIGLE_ACIPP, EnumTypeCollectivite.SIGLE_CIR, EnumTypeCollectivite.SIGLE_S_ACI);
			final List<Operateur> operateurs = serviceSecuriteService.getUtilisateurs(colls);
			if (operateurs != null) {
				for (Operateur operateur : operateurs) {
					if (operateur.getCode().toLowerCase().startsWith(term) || StringComparator.toLowerCaseWithoutAccent(operateur.getNom()).startsWith(term) ||
							StringComparator.toLowerCaseWithoutAccent(operateur.getPrenom()).startsWith(term)) {
						final String label = operateur.getNom() + " " + operateur.getPrenom();
						list.add(new Item(label, label + " (" + operateur.getCode() + ")", operateur.getCode(), String.valueOf(operateur.getIndividuNoTechnique())));
					}
					if (list.size() >= 50) { // [SIFISC-482] on limite à 50 le nombre de résultats retournés
						break;
					}
				}
			}
		}

		return list.toJson();
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