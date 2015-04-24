package ch.vd.uniregctb.migration.pm;

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Résultat de la migration d'un graphe<br/>
 * <ul>
 *     <li>mapping entre numéro de contribuable des pm/établissements... et les données de regpm</li>
 *     <li>contributions aux listes de contrôle à sortir...</li>
 *     <li>...</li>
 * </ul>
 */
// TODO mettre quelque chose dedans !
public class MigrationResult {

	/**
	 * Enumération des différentes listes de contrôle
	 * // TODO il y en a sûrement d'autres...
	 */
	public static enum CategorieListe {
		ADRESSE_DIFFERENTE,

	}

	private final Map<CategorieListe, List<String>> msgs = new EnumMap<>(CategorieListe.class);

	/**
	 * Appelé lors de la migration dès qu'un message doit sortir dans une liste de contrôle
	 * @param cat la catégorie du message (= la liste de contrôle concernée)
	 * @param msg le message
	 */
	public void addMessage(CategorieListe cat, String msg) {
		final List<String> liste = getOrCreateMessageList(cat);
		liste.add(msg);
	}

	private List<String> getOrCreateMessageList(CategorieListe cat) {
		List<String> liste = msgs.get(cat);
		if (liste == null) {
			liste = new LinkedList<>();
			msgs.put(cat, liste);
		}
		return liste;
	}

}
