package ch.vd.unireg.lr.view;

/**
 * Raison pour laquelle le résultat de recherche des LR doit être vide, avec le texte d'explication correspondant
 */
public enum SearchResultsHidingCause {

	/**
	 * Utilisé quand les critères de recherche sont tous vides
	 */
	EMPTY_CRITERIA("error.criteres.vide"),

	/**
	 * Utilisé quand l'un au moins des critères de recherche est invalide
	 */
	INVALID_CRITERIA("error.criteres.invalide");

	private final String messageKey;

	SearchResultsHidingCause(String messageKey) {
		this.messageKey = messageKey;
	}

	public String getMessageKey() {
		return messageKey;
	}
}
