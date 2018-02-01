package ch.vd.unireg.cache;

import org.slf4j.Logger;

import ch.vd.unireg.utils.LogLevel;

public interface KeyDumpableCache {

	/**
	 * Dump dans le logger donné, au niveau donné, la liste des clés du contenu du cache
	 */
	void dumpCacheKeys(Logger logger, LogLevel.Level level);
}
