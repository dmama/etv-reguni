package ch.vd.uniregctb.evenement.civil.ech;

/**
 * Interface utilisée pour récupérer l'information du nombre d'événements civils e-CH
 * sauvegardés en base (= non ignorés) afin de la publier, par exemple, via JMX
 */
public interface EvenementCivilEchReceptionMonitor {

	/**
	 * @return le nombre d'événements civils insérés en base depuis le démarrage de l'application
	 */
	int getNombreEvenementsNonIgnores();

	/**
	 * @return le nombre d'individus actuellement en attente dans la queue des événements à traiter
	 */
	int getNombreIndividusEnAttenteDeTraitement();

	/**
	 * Méthode utilisée dans les tests "live" pour re-demander le traitement de la queue d'événements de l'individu donné
	 * @param noIndividu identifiant de l'individu dont on veut relancer le traitement
	 * @param immediate détermine si oui ou non le décalage temporel doit être appliqué
	 */
	void demanderTraitementQueue(long noIndividu, boolean immediate);
}
