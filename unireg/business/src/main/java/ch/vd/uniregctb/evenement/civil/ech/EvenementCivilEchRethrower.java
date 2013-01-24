package ch.vd.uniregctb.evenement.civil.ech;

/**
 * Interface du composant qui est capable, comme par exemple au démarrage de l'application,
 * de relancer le traitement sur les événements civils eCH "à traiter"
 */
public interface EvenementCivilEchRethrower {

	/**
	 * Récupère les événements civils "à traiter" et tente de les relancer
	 */
	void fetchAndRethrowEvents();
}
