package ch.vd.uniregctb.migration.pm;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.migration.pm.engine.data.ExtractedDataCache;
import ch.vd.uniregctb.migration.pm.engine.log.LogContexte;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LogLevel;
import ch.vd.uniregctb.migration.pm.log.LoggedElement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.utils.EntityKey;

/**
 * Implémentation de l'interface {@link MigrationResultProduction} qui ne fait que collecter les données envoyées,
 * dans un but de contrôle
 */
public class MigrationResultCollector implements MigrationResultContextManipulation, MigrationResultProduction, MigrationResultInitialization {

	public static final class Message {
		public final LogLevel level;
		public final String text;
		public final Map<Class<? extends LoggedElement>, LoggedElement> context;

		public Message(LogLevel level, String text, Map<Class<? extends LoggedElement>, LoggedElement> context) {
			this.level = level;
			this.text = text;
			this.context = Collections.unmodifiableMap(context);
		}
	}

	private final Map<LogCategory, List<Message>> messages = new EnumMap<>(LogCategory.class);
	private final List<Runnable> postTransactionCallbacks = new LinkedList<>();
	private final Map<Class<?>, List<?>> preTransactionCommitData = new HashMap<>();
	private final LogContexte context = new LogContexte();
	private final Graphe currentGraphe;
	private final ExtractedDataCache extractedDataCache;

	public MigrationResultCollector(Graphe graphe) {
		this.currentGraphe = graphe;
		this.extractedDataCache = new ExtractedDataCache(graphe);
	}

	@Override
	public void addMessage(LogCategory cat, LogLevel niveau, String msg) {
		List<Message> forCat = messages.get(cat);
		if (forCat == null) {
			forCat = new LinkedList<>();
			messages.put(cat, forCat);
		}
		forCat.add(new Message(niveau, msg, context.getCurrentContextSnapshot()));
	}

	@Override
	public void addPostTransactionCallback(@NotNull Runnable callback) {
		postTransactionCallbacks.add(callback);
	}

	@Override
	public <D> void addPreTransactionCommitData(@NotNull D data) {
		//noinspection unchecked
		List<D> dataList = (List<D>) preTransactionCommitData.get(data.getClass());
		if (dataList == null) {
			dataList = new LinkedList<>();
			preTransactionCommitData.put(data.getClass(), dataList);
		}
		dataList.add(data);
	}

	/**
	 * @return les messages postés
	 */
	public Map<LogCategory, List<Message>> getMessages() {
		return messages;
	}

	/**
	 * @return les callbacks "post-transaction" enregistrés
	 */
	public List<Runnable> getPostTransactionCallbacks() {
		return postTransactionCallbacks;
	}

	/**
	 * @return les données "pré-transaction commit" enregistrées
	 */
	public Map<Class<?>, List<?>> getPreTransactionCommitData() {
		return preTransactionCommitData;
	}

	@Override
	public <E extends LoggedElement> void pushContextValue(Class<E> clazz, @NotNull E value) {
		context.pushContextValue(clazz, value);
	}

	@Override
	public <E extends LoggedElement> void popContexteValue(Class<E> clazz) {
		context.popContextValue(clazz);
	}

	@Override
	public Graphe getCurrentGraphe() {
		return currentGraphe;
	}

	@Override
	public <D> void registerPreTransactionCommitCallback(Class<D> dataClass,
	                                                     ConsolidationPhase consolidationPhaseIndicator,
	                                                     Function<? super D, ?> keyExtractor,
	                                                     BinaryOperator<D> dataMerger,
	                                                     Consumer<? super D> consolidator) {

		// on ne fait rien ici, de toute façon on n'utilise pas vraiment ces données, on
		// ne fait que collecter les appels à addPreTransactionCommitData...
	}

	/**
	 * Enregistre une méthode d'extraction de données depuis les données RegPM (l'idée est de ne la calculer qu'une seule fois,
	 * ces extracteurs ne seront appelés qu'une seule fois par instance de graphe) utilisable ensuite au travers de la méthode
	 * {@link MigrationResultProduction#getExtractedData(Class, EntityKey)}
	 * @param dataClass classe discriminante pour la donnée à extraire (une donnée par classe et entité)
	 * @param entrepriseExtractor l'extracteur à utiliser si cette données est extraite d'une entreprise
	 * @param etablissementExtractor l'extracteur à utiliser si cette données est extraite d'un établissement
	 * @param individuExtractor l'extracteur à utiliser si cette données est extraite d'un individu
	 * @param <D> le type de la donnée extraite
	 */
	@Override
	public <D> void registerDataExtractor(Class<D> dataClass,
	                                      @Nullable Function<? super RegpmEntreprise, ? extends D> entrepriseExtractor,
	                                      @Nullable Function<? super RegpmEtablissement, ? extends D> etablissementExtractor,
	                                      @Nullable Function<? super RegpmIndividu, ? extends D> individuExtractor) {

		// on laisse faire le pro
		extractedDataCache.registerDataExtractor(dataClass, entrepriseExtractor, etablissementExtractor, individuExtractor);
	}

	@Override
	public <T> T getExtractedData(Class<T> clazz, EntityKey key) {
		// on laisse faire le pro
		return extractedDataCache.getExtractedData(clazz, key);
	}
}
