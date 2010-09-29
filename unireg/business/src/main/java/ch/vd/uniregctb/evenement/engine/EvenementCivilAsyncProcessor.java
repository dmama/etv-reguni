package ch.vd.uniregctb.evenement.engine;

/**
 * Interface publique du composant qui gère le traitement
 * des événements civils
 */
public interface EvenementCivilAsyncProcessor {

	/**
	 * Appelé à la réception d'un événement civil : place celui-ci dans une queue
	 * d'attente en vue du traitement
	 * @param evtId ID technique de l'événement civil à traiter
	 */
	void postEvenementCivil(long evtId);

	/**
	 * @return le nombre d'éléments actuellement en attente dans la queue
	 */
	int getQueueSize();

	/**
	 * Délai, en secondes, de latence pour s'assurer que les événements dans la queue sont bien triés dans le bon ordre
	 * @return le délai (en secondes) d'attente avant de commencer à trier les événements en arrivée
	 */
	int getDelaiPriseEnCompte();

	/**
	 * @return le nombre d'événements civils reçus (au travers de la méthode {@link #postEvenementCivil(long) postEvenementCivil})
	 * depuis le démarrage du service
	 */
	int getNombreEvenementsRecus();

	/**
	 * @return le nombre d'événements civils traités depuis le démarrage du service
	 */
	int getNombreEvenementsTraites();

	/**
	 * Délai, en secondes, de latence pour s'assurer que les événements dans la queue sont bien triés dans le bon ordre
	 * @param delai le délai (en secondes) d'attente avant de commencer à trier les événements en arrivée
	 * @throws IllegalArgumentException si le delai est négatif ou nul
	 */
	void setDelaiPriseEnCompte(int delai);

	/**
	 * Méthode qui attend tant qu'il y a encore des événements civils à traiter.<p/>
	 * Elle n'empêche en aucun cas que d'autres événements civils soient postés, l'attente peut donc s'étendre indéfiniment.<p/>
	 * A priori utilisée dans un contexte de tests pour synchroniser le traitement.
	 * @throws InterruptedException si l'attente a été interrompue par un autre thread
	 */
	void sync() throws InterruptedException;
}
