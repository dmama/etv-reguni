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
	 * Vide et réinitialise le cache pour retrouver son état au démarrage de l'application.
	 */
	void reset();
}
