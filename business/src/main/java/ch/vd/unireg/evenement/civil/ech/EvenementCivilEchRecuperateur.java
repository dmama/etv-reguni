package ch.vd.unireg.evenement.civil.ech;

/**
 * Interface du composant qui est capable, comme par exemple au démarrage de l'application,
 * de récupérer les numéros d'individu sur les événements civils eCH "à traiter" afin de les
 * insérer proprement dans la mécanique de traitement
 */
public interface EvenementCivilEchRecuperateur {

	/**
	 * Récupère les événements civils "à traiter" (assignation du numéro d'individu)
	 */
	void recupererEvenementsCivil();
}
