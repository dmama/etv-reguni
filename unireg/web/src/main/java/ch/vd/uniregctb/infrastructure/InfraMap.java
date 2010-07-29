package ch.vd.uniregctb.infrastructure;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import ch.vd.registre.base.utils.NotImplementedException;

/**
 * Expose les données du service infrastructure sur forme de map.
 */
public class InfraMap<T> implements Map<Integer, T> {

	public static interface Getter<T> {
		T get(Integer numero);

		Collection<T> getAll();
	}

	private final Getter<T> getter;

	public InfraMap(Getter<T> getter) {
		this.getter = getter;
	}

	public void clear() {
		throwReadOnlyException();
	}

	public boolean containsKey(Object key) {
		return get(key) != null;
	}

	public boolean containsValue(Object value) {
		throw new NotImplementedException();
	}

	public Set<java.util.Map.Entry<Integer, T>> entrySet() {
		throw new NotImplementedException();
	}

	public T get(Object key) {
		if (key == null) {
			return null;
		}
		else {
			return getter.get((Integer) key);
		}
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public Set<Integer> keySet() {
		throw new NotImplementedException();
	}

	public T put(Integer key, T value) {
		return throwReadOnlyException();
	}

	public void putAll(Map<? extends Integer, ? extends T> t) {
		throwReadOnlyException();
	}

	public T remove(Object key) {
		return throwReadOnlyException();
	}

	public int size() {
		return values().size();
	}

	public Collection<T> values() {
		return getter.getAll();
	}

	private T throwReadOnlyException() {
		throw new RuntimeException("Cette map est read-only");
	}
}
