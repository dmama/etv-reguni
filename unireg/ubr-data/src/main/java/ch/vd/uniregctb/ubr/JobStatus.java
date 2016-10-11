package ch.vd.uniregctb.ubr;

/**
 * Etat d'un job tel que présenté par le WS batch
 */
public enum JobStatus {
	/**
	 * Terminé sur une exception
	 */
	EXCEPTION,

	/**
	 * Terminé sur une interruption utilisateur - résultats présents mais incomplets
	 */
	INTERRUPTED,

	/**
	 * En cours d'arrêt suite à une demande d'interruption utilisateur
	 */
	INTERRUPTING,

	/**
	 * Terminé normalement (ou jamais démarré)
	 */
	OK,

	/**
	 * En cours d'exécution
	 */
	RUNNING
}
