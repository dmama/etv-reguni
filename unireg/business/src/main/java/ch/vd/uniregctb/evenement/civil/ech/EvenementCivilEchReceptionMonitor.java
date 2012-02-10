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
}
