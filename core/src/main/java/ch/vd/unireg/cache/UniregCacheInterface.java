package ch.vd.unireg.cache;

/**
 * Interface que doivent implémenter tous les caches d'Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface UniregCacheInterface {

	/**
	 * @return la description humainement compréhensible du cache.
	 */
	String getDescription();

	/**
	 * @return construit et retourne les statistiques d'accès au cache
	 */
	CacheStats buildStats();

	/**
	 * Vide et réinitialise le cache pour retrouver son état au démarrage de l'application.
	 */
	void reset();
}
