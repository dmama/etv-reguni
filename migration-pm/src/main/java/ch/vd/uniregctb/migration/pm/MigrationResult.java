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
 *     <li>contributions aux listes de contrôle à sortir...</li>
 *     <li>callback à lancer une fois la transaction committée...</li>
 *     <li>...</li>
 * </ul>
 */
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
		 * Erreurs inattendues, par exemple, messages génériques en général (??)...
		 */
		GENERIQUE,

		/**
		 * Erreurs/messages liés à la migration des adresses
		 */
		ADRESSES,

		/**
		 * Erreurs/messages liés à la migration des individus PM
		 */
		INDIVIDUS_PM,

		/**
		 * Erreurs/messages liés à la migration des fors
		 */
		FORS
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

	/**
	 * Runnables enregistrables pendant la transaction, qui sont lancés une fois la transaction terminée (avec succès)
	 */
	private final List<Runnable> postTransactionCallbacks = new LinkedList<>();

	/**
	 * Les messages à loggeur, relatifs à une migration
	 */
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

		// d'abord, les messages enregistrés localement doivent être repris au niveau global avec un préfixe
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

		// les callbacks enregistrés localement doivent être propagés tout en haut
		postTransactionCallbacks.addAll(results.postTransactionCallbacks);
	}

	/**
	 * Enregistre un callback à appeler une fois la transaction courante committée
	 * @param callback appelé une fois la transaction committée (l'appel se fera hors de tout contexte transactionnel !)
	 */
	public void addPostTransactionCallback(@NotNull Runnable callback) {
		postTransactionCallbacks.add(callback);
	}

	/**
	 * Appelle un a un tous les callbacks précédemment enregistrés, et vide la liste
	 */
	public void runPostTransactionCallbacks() {
		postTransactionCallbacks.forEach(Runnable::run);
		postTransactionCallbacks.clear();
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
