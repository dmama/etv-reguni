package ch.vd.uniregctb.cache;

import net.sf.ehcache.Ehcache;

/**
 * Interface que doivent implémenter tous les caches d'Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface UniregCacheInterface {

	/**
	 * @return le nom (= la clé) du cache.
	 */
	String getName();

	/**
	 * @return la description humainement compréhensible du cache.
	 */
	String getDescription();

	/**
	 * @return le ehcache sous-jacent
	 */
	Ehcache getEhCache();

	/**
	 * Vide et réinitialise le cache pour retrouver son état au démarrage de l'application.
	 */
	void reset();
}
