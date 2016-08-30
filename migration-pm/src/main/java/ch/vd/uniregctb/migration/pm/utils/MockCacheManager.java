package ch.vd.uniregctb.migration.pm.utils;

import java.util.Collection;

import ch.vd.uniregctb.cache.UniregCacheInterface;
import ch.vd.uniregctb.cache.UniregCacheManager;

/**
 * Classe d'implémentation bidon pour le cache manager (qui ne sert pas dans la migration)
 */
public class MockCacheManager implements UniregCacheManager {

	@Override
	public void register(UniregCacheInterface cache) {
	}

	@Override
	public void unregister(UniregCacheInterface cache) {
	}

	@Override
	public Collection<UniregCacheInterface> getCaches() {
		return null;
	}

	@Override
	public UniregCacheInterface getCache(String name) {
		return null;
	}
}
