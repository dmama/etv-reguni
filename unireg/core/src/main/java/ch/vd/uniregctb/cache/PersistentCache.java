package ch.vd.uniregctb.cache;

import java.io.Serializable;
import java.util.Map;

public interface PersistentCache<T extends Serializable> {

	T get(ObjectKey key);
	void put(ObjectKey key, T object);
	void putAll(Map<? extends ObjectKey, T> map);
	void removeAll(long id);
	void clear();

	/**
	 * @return construit et retourne les statistiques d'accès au cache
	 */
	CacheStats buildStats();
}
