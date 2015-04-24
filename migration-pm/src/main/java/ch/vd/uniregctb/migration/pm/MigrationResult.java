package ch.vd.uniregctb.migration.pm;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

/**
 * Résultat de la migration d'un graphe<br/>
 * <ul>
 *     <li>contributions aux listes de contrôle à sortir...</li>
 *     <li>callback à lancer une fois la transaction committée...</li>
 *     <li>...</li>
 * </ul>
 */
public class MigrationResult implements MigrationResultProduction, MigrationResultMessageProvider {

	/**
	 * Runnables enregistrables pendant la transaction, qui sont lancés une fois la transaction terminée (avec succès)
	 */
	private final List<Runnable> postTransactionCallbacks = new LinkedList<>();

	/**
	 * Les messages à logguer, relatifs à une migration
	 */
	private final Map<MigrationResultMessage.CategorieListe, List<MigrationResultMessage>> msgs = new EnumMap<>(MigrationResultMessage.CategorieListe.class);

	/**
	 * Données maintenues pour les enregistrements de structures de données à consolider
	 * pendant la transaction
	 */
	private final Map<Class<?>, PreTransactionCommitRegistration<?>> preCommitRegistrations = new HashMap<>();

	/**
	 * Appelé lors de la migration dès qu'un message doit sortir dans une liste de contrôle
	 * @param cat la catégorie du message (= la liste de contrôle concernée)
	 * @param niveau le niveau du message
	 * @param msg le message
	 */
	public void addMessage(MigrationResultMessage.CategorieListe cat, MigrationResultMessage.Niveau niveau, String msg) {
		addMessage(cat, new MigrationResultMessage(niveau, msg));
	}

	private void addMessage(MigrationResultMessage.CategorieListe cat, MigrationResultMessage msg) {
		final List<MigrationResultMessage> liste = getOrCreateMessageList(cat);
		liste.add(msg);
	}

	private List<MigrationResultMessage> getOrCreateMessageList(MigrationResultMessage.CategorieListe cat) {
		List<MigrationResultMessage> liste = msgs.get(cat);
		if (liste == null) {
			liste = new LinkedList<>();
			msgs.put(cat, liste);
		}
		return liste;
	}

	@NotNull
	public List<MigrationResultMessage> getMessages(MigrationResultMessage.CategorieListe cat) {
		final List<MigrationResultMessage> found = msgs.get(cat);
		return found == null ? Collections.emptyList() : found;
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

	/**
	 * Enregistre un traitement a effectuer avant la fin de la transaction
	 * @param dataClass classe de la donnée postée (on n'acceptera qu'un seul enregistrement par classe !)
	 * @param keyExtractor extracteur de la clé de regroupement pour les données postées
	 * @param dataMerger fusionneur des données associées à une clé postée plusieurs fois
	 * @param consolidator opération finale à effectuée sur les données consolidées
	 * @param <D> type des données postées et traitées
	 */
	public <D> void registerPreTransactionCommitCallback(Class<D> dataClass,
	                                                     Function<? super D, ?> keyExtractor,
	                                                     BinaryOperator<D> dataMerger,
	                                                     Consumer<? super D> consolidator) {

		if (preCommitRegistrations.containsKey(dataClass)) {
			throw new IllegalArgumentException("Un enregistrement a déjà été fait pour la classe " + dataClass.getName());
		}
		preCommitRegistrations.put(dataClass, new PreTransactionCommitRegistration<>(keyExtractor, dataMerger, consolidator));
	}

	/**
	 * Enregistre une donnée qui sera intégrée aux autres et traitée en fin de transaction
	 * @param data donnée à intégrer
	 * @param <D> type de la donnée
	 */
	public <D> void addPreTransactionCommitData(@NotNull D data) {
		//noinspection unchecked
		final PreTransactionCommitRegistration<D> registration = (PreTransactionCommitRegistration<D>) preCommitRegistrations.get(data.getClass());
		if (registration == null) {
			throw new IllegalArgumentException("Aucun enregistrement n'a été fait pour la classe " + data.getClass().getName());
		}

		final Map<Object, D> dataMap = registration.data;
		final Object key = registration.keyExtractor.apply(data);
		dataMap.merge(key, data, registration.dataMerger);
	}

	/**
	 * Consolide (= appelle le consolidator) pour les données postées
	 */
	public void consolidatePreTransactionCommitRegistrations() {
		for (PreTransactionCommitRegistration<?> registration : preCommitRegistrations.values()) {
			final Map<Object, ?> dataMap = registration.data;
			//noinspection unchecked
			final Consumer<Object> consolidator = (Consumer<Object>) registration.consolidator;
			dataMap.values().forEach(consolidator);
		}
	}

	@Override
	public String toString() {

		final class Denormalized extends MigrationResultMessage {
			final CategorieListe cat;

			Denormalized(Niveau niveau, String texte, CategorieListe cat) {
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

	/**
	 * Données maintenues pour un type de données à consolider
	 * @param <D> type des données à consolider
	 */
	private static final class PreTransactionCommitRegistration<D> {
		final Function<? super D, ?> keyExtractor;
		final BinaryOperator<D> dataMerger;
		final Consumer<? super D> consolidator;
		final Map<Object, D> data;

		public PreTransactionCommitRegistration(Function<? super D, ?> keyExtractor, BinaryOperator<D> dataMerger, Consumer<? super D> consolidator) {
			this.keyExtractor = keyExtractor;
			this.dataMerger = dataMerger;
			this.consolidator = consolidator;
			this.data = new LinkedHashMap<>();      // pour garder l'ordre, en test...
		}
	}
}
