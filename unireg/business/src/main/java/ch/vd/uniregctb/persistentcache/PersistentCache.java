package ch.vd.uniregctb.persistentcache;

import java.io.Serializable;

public interface PersistentCache<T extends Serializable> {

	T get(ObjectKey key);
	void put(ObjectKey key, T object);
	void removeAll(long id);
	void clear();
}
