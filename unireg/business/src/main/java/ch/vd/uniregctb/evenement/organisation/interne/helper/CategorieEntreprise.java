package ch.vd.uniregctb.evenement.organisation.interne.helper;

/**
 * Catégories métier d'entreprises pertinentes pour Unireg.
 *
 * Permet notamment de classifier les entreprises visées par un événement RCEnt et determiner
 * les actions à entreprendre.
 *
 * Spécification:
 *  - Ti01SE03-Identifier et traiter les mutations entreprise.doc - Version 0.6 - 08.09.2015
 *
 * @author Raphaël Marmier, 2015-09-08
 */
public enum CategorieEntreprise {

	PP("Société individuelle"),

	SP("Société de personnes"),

	PM("Personne morale"),

	APM("Association et Fondation"),

	FDS_PLAC("Fonds de placement"),

	DP_PM("Personne morale de droit public"),

	DP_APM("Association de droit public");

	private String nomLong;

	CategorieEntreprise(String nomLong) {
		this.nomLong = nomLong;
	}

	@Override
	public String toString() {
		return nomLong;
	}
}
