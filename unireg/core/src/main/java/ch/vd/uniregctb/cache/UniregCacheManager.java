package ch.vd.uniregctb.cache;

import java.util.Collection;

/**
 * Cache manager dans lequel doivent venir s'enregistrer les caches d'Unireg
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface UniregCacheManager {

	void register(UniregCacheInterface cache);

	void unregister(UniregCacheInterface cache);

	Collection<UniregCacheInterface> getCaches();

	UniregCacheInterface getCache(String name);
}
