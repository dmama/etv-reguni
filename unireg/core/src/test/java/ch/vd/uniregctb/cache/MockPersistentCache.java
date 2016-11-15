package ch.vd.uniregctb.cache;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.Predicate;

import ch.vd.registre.base.utils.NotImplementedException;

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
		final Iterator<Map.Entry<ObjectKey, T>> iterator = map.entrySet().iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getKey().getId() == id) {
				iterator.remove();
			}
		}
	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public void removeValues(Predicate<? super T> removal) {
		final Iterator<Map.Entry<ObjectKey, T>> iterator = map.entrySet().iterator();
		while (iterator.hasNext()) {
			if (removal.evaluate(iterator.next().getValue())) {
				iterator.remove();
			}
		}
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
		throw new NotImplementedException();
	}
}
