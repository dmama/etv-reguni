package ch.vd.uniregctb.cache;

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
	 * @return construit et retourne les statistiques d'accès au cache
	 */
	CacheStats buildStats();

	/**
	 * Vide et réinitialise le cache pour retrouver son état au démarrage de l'application.
	 */
	void reset();

	/**
	 * @return une chaîne de caractères qui reprend toutes les clés présentes dans le cache, directement postable dans les logs
	 */
	String dump();
}
