package ch.vd.uniregctb.cache;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public interface KeyValueDumpableCache {

	/**
	 * Dump dans le logger donné, au niveau donné, la liste des couples clé/valeur du contenu du cache
	 */
	void dumpCacheContent(Logger logger, Level level);
}
