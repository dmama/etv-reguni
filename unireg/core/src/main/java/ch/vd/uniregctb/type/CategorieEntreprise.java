package ch.vd.uniregctb.type;

/**
 * Les différentes catégories d'entreprises connues dans Unireg
 */
public enum CategorieEntreprise {

	PP("Société individuelle"),
	SP("Société de personnes"),
	PM("Personne morale"),            // TODO <-- ne faudrait-il pas changer ce nom qui porte à confusion ?
	APM("Association/fondation"),
	FP("Fonds de placement"),
	DPPM("Personne morale de droit public"),      // DP/PM
	DP("Institut de droit public"),               // DP/APM
	AUTRE("-");

	private final String libelle;

	CategorieEntreprise(String libelle) {
		this.libelle = libelle;
	}

	public String getLibelle() {
		return libelle;
	}
}
