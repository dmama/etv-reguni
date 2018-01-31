package ch.vd.uniregctb.evenement.registrefoncier;

public enum EtatEvenementRF {

	/**
	 * L'événement doit être traité.
	 */
	A_TRAITER,
	/**
	 * L'événement est en cours de traitement.
	 */
	EN_TRAITEMENT,
	/**
	 * L'événement a été traité avec succès.
	 */
	TRAITE,
	/**
	 * L'événement est en erreur.
	 */
	EN_ERREUR,
	/**
	 * L'événement a été forcé, c'est-à-dire que le traitement métier n'a pas été effectué automatiquement (il devrait être fait à la main).
	 */
	FORCE
}
