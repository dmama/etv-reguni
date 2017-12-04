package ch.vd.uniregctb.common.linkedentity;

public enum LinkedEntityPhase {
	/**
	 * On demande les entités liées dans le context de la validation des données avant sauvegarde dans la base. Les entités retournées seront elles-mêmes validées.
	 */
	VALIDATION,
	/**
	 * On demande les entités liées dans le context de l'indexation des données après sauvegarde dans la base. Les entités retournées seront elles-mêmes indexées.
	 */
	INDEXATION,
	/**
	 * On demande les entités liées dans le context du recalcul des parentés entre personnes physiques.
	 */
	PARENTES,
	/**
	 * On demande les entités liées dans le context du recalcul des tâches sur les contribuables.
	 */
	TACHES,
	/**
	 * On demande les entités liées dans le context de l'envoi d'événements de changement internes (pour de l'invalidation des divers caches applicatifs, entres autres). Les entités retournées provoqueront l'émission d'autant d'événements
	 * internes.
	 */
	DATA_EVENT
}
