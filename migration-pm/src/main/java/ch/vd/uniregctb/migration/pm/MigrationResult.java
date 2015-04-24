package ch.vd.uniregctb.migration.pm;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

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
		/**
		 * Erreurs inattendues, par exemple
		 */
		ERREUR_GENERIQUE,

		/**
		 * L'adresse trouvée dans RegPM n'est pas équivalente à celle de RCEnt
		 */
		ADRESSE_DIFFERENTE,

	}

	public static enum NiveauMessage {
		DEBUG,
		INFO,
		WARN,
		ERROR
	}

	private final Map<CategorieListe, List<Pair<NiveauMessage, String>>> msgs = new EnumMap<>(CategorieListe.class);

	/**
	 * Appelé lors de la migration dès qu'un message doit sortir dans une liste de contrôle
	 * @param cat la catégorie du message (= la liste de contrôle concernée)
	 * @param niveau le niveau du message
	 * @param msg le message
	 */
	public void addMessage(CategorieListe cat, NiveauMessage niveau, String msg) {
		final List<Pair<NiveauMessage, String>> liste = getOrCreateMessageList(cat);
		liste.add(Pair.of(niveau, msg));
	}

	private List<Pair<NiveauMessage, String>> getOrCreateMessageList(CategorieListe cat) {
		List<Pair<NiveauMessage, String>> liste = msgs.get(cat);
		if (liste == null) {
			liste = new LinkedList<>();
			msgs.put(cat, liste);
		}
		return liste;
	}

	@NotNull
	public List<Pair<NiveauMessage, String>> getMessages(CategorieListe cat) {
		final List<Pair<NiveauMessage, String>> found = msgs.get(cat);
		return found == null ? Collections.emptyList() : found;
	}
}
