package ch.vd.uniregctb.cache;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public interface KeyDumpableCache {

	/**
	 * Dump dans le logger donné, au niveau donné, la liste des clés du contenu du cache
	 */
	void dumpCacheKeys(Logger logger, Level level);
}
