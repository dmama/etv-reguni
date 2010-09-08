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
	 * @param timestamp timestamp (voir {@link System#currentTimeMillis()}) de l'arrivée de l'événement
	 * @param interested si non-null, l'objet qu'il faut 'notifier' une fois le traitement terminé (appel à {@link Object#notifyAll()}, pour les tests seulement!)
	 */
	void postEvenementCivil(long evtId, long timestamp, Object interested);

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
	 * Délai, en secondes, de latence pour s'assurer que les événements dans la queue sont bien triés dans le bon ordre
	 * @param delai le délai (en secondes) d'attente avant de commencer à trier les événements en arrivée
	 * @throws IllegalArgumentException si le delai est négatif ou nul
	 */
	void setDelaiPriseEnCompte(int delai);

}
