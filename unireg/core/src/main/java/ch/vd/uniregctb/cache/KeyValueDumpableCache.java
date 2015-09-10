package ch.vd.uniregctb.cache;

import org.slf4j.Logger;

import ch.vd.uniregctb.utils.LogLevel;

public interface KeyValueDumpableCache {

	/**
	 * Dump dans le logger donné, au niveau donné, la liste des couples clé/valeur du contenu du cache
	 */
	void dumpCacheContent(Logger logger, LogLevel.Level level);
}
