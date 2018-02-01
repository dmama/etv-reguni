package ch.vd.uniregctb.reqdes;

/**
 * Type d'inscription d'une transaction immobilière sur un acte désigné
 * <p/>
 * Longueur de colonne : 15
 */
public enum TypeInscription {
	PROPRIETE("Propriété"),
	SERVITUDE("Servitude"),
	CHARGE_FONCIERE("Charge foncière"),
	ANNOTATION("Annotation");

	private final String displayName;

	TypeInscription(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}