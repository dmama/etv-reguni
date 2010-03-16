package ch.vd.uniregctb.cache;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ch.vd.registre.base.utils.Assert;

public class UniregCacheManagerImpl implements UniregCacheManager {

	private final Map<String, UniregCacheInterface> map = new HashMap<String, UniregCacheInterface>();

	public UniregCacheInterface getCache(String name) {
		return map.get(name);
	}

	public Collection<UniregCacheInterface> getCaches() {
		return map.values();
	}

	public void register(UniregCacheInterface cache) {
		Assert.isFalse(map.containsKey(cache.getName()));
		map.put(cache.getName(), cache);
	}

	public void unregister(UniregCacheInterface cache) {
		map.remove(cache);
	}
}
