package ch.vd.uniregctb.evenement.externe;

/**
 * Longueur de colonne : 10
 */
public enum EtatEvenementExterne {

	/**
	 * Evénement non-traité, i.e., en attente de traitement
	 */
	NON_TRAITE,

	/**
	 * Evénement traité avec succes
	 */
	TRAITE,

	/**
	 * Evénement traité en erruer
	 */
	ERREUR;


	public String getName() {
		return name();
	}
}
