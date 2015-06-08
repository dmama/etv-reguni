package ch.vd.uniregctb.migration.pm;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

/**
 * Implémentation de l'interface {@link MigrationResultProduction} qui ne fait que collecter les données envoyées,
 * dans un but de contrôle
 */
public class MigrationResultCollector implements MigrationResultProduction {

	private final Map<MigrationResultMessage.CategorieListe, List<MigrationResultMessage>> messages = new EnumMap<>(MigrationResultMessage.CategorieListe.class);
	private final List<Runnable> postTransactionCallbacks = new LinkedList<>();
	private final Map<Class<?>, List<?>> preTransactionCommitData = new HashMap<>();

	@Override
	public void addMessage(MigrationResultMessage.CategorieListe cat, MigrationResultMessage.Niveau niveau, String msg) {
		List<MigrationResultMessage> forCat = messages.get(cat);
		if (forCat == null) {
			forCat = new LinkedList<>();
			messages.put(cat, forCat);
		}
		forCat.add(new MigrationResultMessage(niveau, msg));
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
	public Map<MigrationResultMessage.CategorieListe, List<MigrationResultMessage>> getMessages() {
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
}
