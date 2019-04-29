package ch.vd.unireg.cache;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Mock du persistent cache qui utilise une map en mémoire (!) comme backend.
 */
public class MockPersistentCache<T extends Serializable> implements PersistentCache<T> {

	private final Map<ObjectKey, T> map = new HashMap<>();

	@Override
	public T get(ObjectKey key) {
		return map.get(key);
	}

	@Override
	public void put(ObjectKey key, T object) {
		map.put(key, object);
	}

	@Override
	public void putAll(Map<? extends ObjectKey, T> map) {
		this.map.putAll(map);
	}

	@Override
	public void removeAll(long id) {
		map.entrySet().removeIf(objectKeyTEntry -> objectKeyTEntry.getKey().getId() == id);
	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public void removeValues(Predicate<? super T> removal) {
		map.entrySet().removeIf(objectKeyTEntry -> removal.evaluate(objectKeyTEntry.getValue()));
	}

	@Override
	public Set<Map.Entry<ObjectKey, T>> entrySet() {
		return map.entrySet();
	}

	@Override
	public Set<ObjectKey> keySet() {
		return map.keySet();
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public CacheStats buildStats() {
		throw new NotImplementedException("");
	}
}
