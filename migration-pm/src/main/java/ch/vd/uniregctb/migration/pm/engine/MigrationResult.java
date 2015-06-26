package ch.vd.uniregctb.migration.pm.engine;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.migration.pm.Graphe;
import ch.vd.uniregctb.migration.pm.MigrationResultContextManipulation;
import ch.vd.uniregctb.migration.pm.MigrationResultInitialization;
import ch.vd.uniregctb.migration.pm.MigrationResultMessageProvider;
import ch.vd.uniregctb.migration.pm.MigrationResultProduction;
import ch.vd.uniregctb.migration.pm.engine.data.ExtractedDataCache;
import ch.vd.uniregctb.migration.pm.engine.log.LogContexte;
import ch.vd.uniregctb.migration.pm.engine.log.LogStructure;
import ch.vd.uniregctb.migration.pm.log.CompositeLoggedElement;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LogLevel;
import ch.vd.uniregctb.migration.pm.log.LoggedElement;
import ch.vd.uniregctb.migration.pm.log.LoggedElementAttribute;
import ch.vd.uniregctb.migration.pm.log.MessageLoggedElement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.utils.EntityKey;

/**
 * Résultat de la migration d'un graphe<br/>
 * <ul>
 *     <li>contributions aux listes de contrôle à sortir...</li>
 *     <li>callback à lancer une fois la transaction committée...</li>
 *     <li>...</li>
 * </ul>
 */
public class MigrationResult implements MigrationResultProduction, MigrationResultInitialization, MigrationResultContextManipulation, MigrationResultMessageProvider {

	/**
	 * Runnables enregistrables pendant la transaction, qui sont lancés une fois la transaction terminée (avec succès)
	 * @see #addPostTransactionCallback(Runnable)
	 */
	private final List<Runnable> postTransactionCallbacks = new LinkedList<>();

	/**
	 * Les messages à logguer, relatifs à une migration
	 * @see #addMessage(LogCategory, LogLevel, String)
	 */
	private final Map<LogCategory, List<LoggedElement>> msgs = new EnumMap<>(LogCategory.class);

	/**
	 * Données maintenues pour les enregistrements de structures de données à consolider
	 * pendant la transaction
	 * @see #registerPreTransactionCommitCallback(Class, int, Function, BinaryOperator, Consumer)
	 * @see #addPreTransactionCommitData(Object)
	 */
	private final Map<Class<?>, PreTransactionCommitRegistration<?>> preCommitRegistrations = new HashMap<>();

	/**
	 * Ordonnancement des traitements de consolidation des données pendant la transaction
	 */
	private final SortedMap<Integer, Class<?>> preCommitRegistrationSortingOrder = new TreeMap<>();

	/**
	 * Graphe pour lequel la migration est en cours
	 */
	private final Graphe graphe;

	/**
	 * Contexte courant de log (= définit les valeurs à logguer en association avec un nouveau message...)
	 */
	private final LogContexte currentContext = new LogContexte();

	/**
	 * Cache de données extraites sur les entités à migrer
	 */
	private final ExtractedDataCache extractedDataCache;

	/**
	 * @param graphe pour lequel la migration est en cours
	 */
	public MigrationResult(Graphe graphe) {
		this.graphe = graphe;
		this.extractedDataCache = new ExtractedDataCache(graphe);
	}

	@Override
	public Graphe getCurrentGraphe() {
		return graphe;
	}

	/**
	 * Appelé lors de la migration dès qu'un message doit sortir dans une liste de contrôle
	 * @param cat la catégorie du message (= la liste de contrôle concernée)
	 * @param niveau le niveau du message
	 * @param msg le message
	 */
	public void addMessage(LogCategory cat, LogLevel niveau, String msg) {
		final List<LoggedElement> contexte = LogStructure.resolveContextForCategory(currentContext, cat);
		final List<LoggedElement> local = Stream.concat(contexte.stream(), Stream.of(new MessageLoggedElement(niveau, msg))).collect(Collectors.toList());

		final List<LoggedElement> liste = getOrCreateMessageList(cat);
		liste.add(new CompositeLoggedElement(local));
	}

	@NotNull
	private List<LoggedElement> getOrCreateMessageList(LogCategory cat) {
		List<LoggedElement> liste = msgs.get(cat);
		if (liste == null) {
			liste = new LinkedList<>();
			msgs.put(cat, liste);
		}
		return liste;
	}

	/**
	 * @param category une catégorie de log
	 * @return la liste des attributs loggués (= le nom des colonnes du résultat, au final...) pour cette catégorie
	 */
	@NotNull
	public static List<LoggedElementAttribute> getColumns(LogCategory category) {
		final LogContexte emptyContexte = new LogContexte();
		final List<LoggedElement> contexte = LogStructure.resolveContextForCategory(emptyContexte, category);
		final List<LoggedElement> all = Stream.concat(contexte.stream(), Stream.of(MessageLoggedElement.EMPTY)).collect(Collectors.toList());
		final LoggedElement composite = new CompositeLoggedElement(all);
		return composite.getItems();
	}

	@NotNull
	public List<LoggedElement> getMessages(LogCategory cat) {
		final List<LoggedElement> found = msgs.get(cat);
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
	 * Appelle un à un tous les callbacks précédemment enregistrés, et vide la liste
	 */
	public void runPostTransactionCallbacks() {
		postTransactionCallbacks.forEach(Runnable::run);
		postTransactionCallbacks.clear();
	}

	/**
	 * Enregistre un traitement a effectuer avant la fin de la transaction
	 * @param dataClass classe de la donnée postée (on n'acceptera qu'un seul enregistrement par classe !)
	 * @param consolidationPhaseIndicator indicateur de l'emplacement de cette consolidation dans la grande liste des consolidations
	 * @param keyExtractor extracteur de la clé de regroupement pour les données postées
	 * @param dataMerger fusionneur des données associées à une clé postée plusieurs fois
	 * @param consolidator opération finale à effectuée sur les données consolidées
	 * @param <D> type des données postées et traitées
	 */
	public <D> void registerPreTransactionCommitCallback(Class<D> dataClass,
	                                                     int consolidationPhaseIndicator,
	                                                     Function<? super D, ?> keyExtractor,
	                                                     BinaryOperator<D> dataMerger,
	                                                     Consumer<? super D> consolidator) {

		if (preCommitRegistrations.containsKey(dataClass)) {
			throw new IllegalArgumentException("Un enregistrement a déjà été fait pour la classe " + dataClass.getName());
		}
		if (preCommitRegistrationSortingOrder.containsKey(consolidationPhaseIndicator)) {
			throw new IllegalArgumentException("Le numéro de phase " + consolidationPhaseIndicator + " a déjà été utilisé.");
		}
		preCommitRegistrations.put(dataClass, new PreTransactionCommitRegistration<>(keyExtractor, dataMerger, consolidator));
		preCommitRegistrationSortingOrder.put(consolidationPhaseIndicator, dataClass);
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
	 * Consolide (= appelle le consolidator) pour toutes les données postées, en tenant compte de l'ordonnancement
	 * entre les différentes consolidations
	 */
	public void consolidatePreTransactionCommitRegistrations() {
		preCommitRegistrationSortingOrder.values().stream()
				.map(preCommitRegistrations::get)
				.forEach(registration -> {
					final Map<Object, ?> dataMap = registration.data;
					//noinspection unchecked
					final Consumer<Object> consolidator = (Consumer<Object>) registration.consolidator;
					dataMap.values().forEach(consolidator);
				});
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

	//
	// Gestion du contexte de log
	//

	@Override
	public <E extends LoggedElement> void setContextValue(Class<E> clazz, @NotNull E value) {
		currentContext.setContextValue(clazz, value);
	}

	@Override
	public <E extends LoggedElement> void resetContextValue(Class<E> clazz) {
		currentContext.resetContextValue(clazz);
	}

	//
	// Gestion des extracteurs de données
	//

	@Override
	public <D> void registerDataExtractor(Class<D> dataClass,
	                                      @Nullable Function<? super RegpmEntreprise, ? extends D> entrepriseExtractor,
	                                      @Nullable Function<? super RegpmEtablissement, ? extends D> etablissementExtractor,
	                                      @Nullable Function<? super RegpmIndividu, ? extends D> individuExtractor) {
		// on laisse faire le pro
		extractedDataCache.registerDataExtractor(dataClass, entrepriseExtractor, etablissementExtractor, individuExtractor);
	}

	@NotNull
	@Override
	public <T> T getExtractedData(Class<T> clazz, EntityKey key) {
		// on laisse faire le pro
		return extractedDataCache.getExtractedData(clazz, key);
	}
}
