package ch.vd.unireg.cache;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.Predicate;

public interface PersistentCache<T extends Serializable> {

	T get(ObjectKey key);
	void put(ObjectKey key, T object);
	void putAll(Map<? extends ObjectKey, T> map);
	void removeAll(long id);
	void clear();
	void removeValues(Predicate<? super T> removal);
	Set<Map.Entry<ObjectKey, T>> entrySet();
	Set<ObjectKey> keySet();
	int size();

	/**
	 * @return construit et retourne les statistiques d'accès au cache
	 */
	CacheStats buildStats();
}
