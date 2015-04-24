package ch.vd.uniregctb.migration.pm;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
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
	public enum CategorieListe {

		/**
		 * Cas ok qui indique qu'une PM est migrée
		 */
		PM_MIGREE,

		/**
		 * Lors de la migration de l'adresse du mainframe, la localité a été "devinée" par rapport à la liste des localités maintenant disponibles dans RefInf pour la même commune
		 * (la rue n'a été trouvée dans aucune des localités existantes)
		 */
		LOCALITE_DEVINEE,

		/**
		 * Erreurs inattendues, par exemple, messages génériques en général (??)...
		 */
		GENERIQUE,

		/**
		 * L'adresse trouvée dans RegPM n'est pas équivalente à celle de RCEnt
		 */
		ADRESSE_DIFFERENTE,

	}

	public enum NiveauMessage {
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
		addMessage(cat, new Message(niveau, msg));
	}

	private void addMessage(CategorieListe cat, Message msg) {
		final List<Message> liste = getOrCreateMessageList(cat);
		liste.add(msg);
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

	public void addAll(MigrationResult results, String prefixe) {
		results.msgs.entrySet().forEach(entry -> {
			final CategorieListe cat = entry.getKey();
			final List<Message> newMessages;
			if (StringUtils.isNotBlank(prefixe)) {
				newMessages = entry.getValue().stream()
						.map(source -> new Message(source.niveau, String.format("%s : %s", prefixe, source.texte)))
						.collect(Collectors.toList());
			}
			else {
				newMessages = entry.getValue();
			}
			if (newMessages != null) {
				newMessages.forEach(msg -> addMessage(cat, msg));
			}
		});
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
