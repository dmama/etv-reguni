package ch.vd.uniregctb.cache;

public interface DumpableUniregCache extends UniregCacheInterface {

	/**
	 * @return une chaîne de caractères qui reprend toutes les clés présentes dans le cache, directement postable dans les logs
	 */
	String dumpCacheKeys();
}
