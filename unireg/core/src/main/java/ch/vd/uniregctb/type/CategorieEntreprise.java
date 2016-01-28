package ch.vd.uniregctb.type;

/**
 * Les différentes catégories d'entreprises connues dans Unireg
 */
public enum CategorieEntreprise {

	PP("Société individuelle"),
	SP("SP - SC et SNC"),
	PM("PM - Société de capitaux"),            // TODO <-- ne faudrait-il pas changer ce nom qui porte à confusion ?
	APM("APM - Association / Fondation"),
	FP("FDS PLAC - Fond de placement"),
	DPPM("DP/PM - Entreprise de droit public"),      // DP/PM
	DPAPM("DP/APM - Administration de droit public"),               // DP/APM
	AUTRE("Autre");

	private final String libelle;

	CategorieEntreprise(String libelle) {
		this.libelle = libelle;
	}

	public String getLibelle() {
		return libelle;
	}
}
