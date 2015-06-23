package ch.vd.uniregctb.migration.pm;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LogLevel;
import ch.vd.uniregctb.migration.pm.log.LoggedElement;

/**
 * Implémentation de l'interface {@link MigrationResultProduction} qui ne fait que collecter les données envoyées,
 * dans un but de contrôle
 */
public class MigrationResultCollector implements MigrationResultContextManipulation, MigrationResultProduction {

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
	private final Map<Class<? extends LoggedElement>, LoggedElement> context = new HashMap<>();

	@Override
	public void addMessage(LogCategory cat, LogLevel niveau, String msg) {
		List<Message> forCat = messages.get(cat);
		if (forCat == null) {
			forCat = new LinkedList<>();
			messages.put(cat, forCat);
		}
		forCat.add(new Message(niveau, msg, getCurrentContextSnapshot()));
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

	@NotNull
	private Map<Class<? extends LoggedElement>, LoggedElement> getCurrentContextSnapshot() {
		return new HashMap<>(context);
	}

	@Override
	public <E extends LoggedElement> void setContextValue(Class<E> clazz, @NotNull E value) {
		context.put(clazz, value);
	}

	@Override
	public <E extends LoggedElement> void resetContextValue(Class<E> clazz) {
		context.remove(clazz);
	}

	@Override
	public Graphe getCurrentGraphe() {
		return null;
	}
}
