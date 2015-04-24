package ch.vd.uniregctb.migration.pm;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
		 * Cas ok qui indique qu'une PM est migrée
		 */
		PM_MIGREE,

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
	
	public static class Message {
		final NiveauMessage niveau;
		final String texte;

		private Message(NiveauMessage niveau, String texte) {
			this.niveau = niveau;
			this.texte = texte;
		}
	}

	private final Map<CategorieListe, List<Message>> msgs = new EnumMap<>(CategorieListe.class);

	/**
	 * Appelé lors de la migration dès qu'un message doit sortir dans une liste de contrôle
	 * @param cat la catégorie du message (= la liste de contrôle concernée)
	 * @param niveau le niveau du message
	 * @param msg le message
	 */
	public void addMessage(CategorieListe cat, NiveauMessage niveau, String msg) {
		final List<Message> liste = getOrCreateMessageList(cat);
		liste.add(new Message(niveau, msg));
	}

	private List<Message> getOrCreateMessageList(CategorieListe cat) {
		List<Message> liste = msgs.get(cat);
		if (liste == null) {
			liste = new LinkedList<>();
			msgs.put(cat, liste);
		}
		return liste;
	}

	@NotNull
	public List<Message> getMessages(CategorieListe cat) {
		final List<Message> found = msgs.get(cat);
		return found == null ? Collections.emptyList() : found;
	}

	@Override
	public String toString() {

		final class Denormalized extends Message {
			final CategorieListe cat;

			Denormalized(NiveauMessage niveau, String texte, CategorieListe cat) {
				super(niveau, texte);
				this.cat = cat;
			}

			@Override
			public String toString() {
				return String.format("cat=%s, niveau=%s, texte='%s'", cat, niveau, texte);
			}
		}

		if (msgs.isEmpty()) {
			return "RAS";
		}

		return msgs.entrySet().stream()
				.map(entry -> entry.getValue().stream().map(msg -> new Denormalized(msg.niveau, msg.texte, entry.getKey())))
				.flatMap(Function.<Stream<Denormalized>>identity())
				.map(Object::toString)
				.collect(Collectors.joining(System.lineSeparator()));
	}
}
