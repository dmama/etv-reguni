package ch.vd.uniregctb.type;

/**
 * Les différentes catégories d'entreprises connues dans Unireg
 */
public enum CategorieEntreprise {

	/**
	 La catégorie n'est pas encore déterminée et est donc inconnue à ce stade.
	 */
	INDET("En attente de détermination"),

	/**
	 * Société de personnes.
	 */
	SP("SP - Société de personnes"),
	/**
	 * Personne morale.
	 */
	PM("PM - Personne morale"),

	/**
	 * Association au sens large.
	 */
	APM("APM - Autre personne morale"),

	/**
	 * Catégorie de compatibilité: la catégorie est définie mais non encore répértoriée dans le service infra utilisé par Unireg. Donne la possibilité de découpler la mise-à-jour d'Unireg de celle du service infra.
	 */
	AUTRE("Autre catégorie (Compatibilité)");

	private final String libelle;

	CategorieEntreprise(String libelle) {
		this.libelle = libelle;
	}

	public String getLibelle() {
		return libelle;
	}
}
