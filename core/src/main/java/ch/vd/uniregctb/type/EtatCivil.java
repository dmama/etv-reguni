package ch.vd.uniregctb.type;

/**
 * Longueur de colonne : 34
 */
public enum EtatCivil {

	CELIBATAIRE("Célibataire"),
	MARIE("Marié(e)"),
	VEUF("Veuf(ve)"),
	LIE_PARTENARIAT_ENREGISTRE("Lié(e) partenariat enregistré"),
	NON_MARIE("Non marié(e)"),
	PARTENARIAT_DISSOUS_JUDICIAIREMENT("Partenariat dissous judiciairement"),
	DIVORCE("Divorcé(e)"),
	SEPARE("Séparé(e)"),
	PARTENARIAT_DISSOUS_DECES("Partenariat dissous décès"),
	PARTENARIAT_SEPARE("Partenariat séparé");

	private final String format;

	EtatCivil(String format) {
		this.format = format;
	}

	public String format() {
		return format;
	}
}
