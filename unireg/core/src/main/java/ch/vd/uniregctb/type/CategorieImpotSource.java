/**
 *
 */
package ch.vd.uniregctb.type;

/** 
 * Longueur de colonne : 32
 * Catégorie de l'impôt à la source selon les art. 130 ss. LI.
 * Valeurs possibles :
 * - Travailleurs
 * - Artistes, sportifs et conférenciers
 * - Administrateurs
 * - Créanciers hypothécaires
 * - Bénéficiaires de prestations de prévoyance découlant de rapports de travail de droit public
 * - Bénéficiaires de prestations de prévoyance découlant du droit privé
 */
public enum CategorieImpotSource {
	ADMINISTRATEURS("Administrateurs"),
	CONFERENCIERS_ARTISTES_SPORTIFS("Conférenciers, artistes, sportifs"),
	CREANCIERS_HYPOTHECAIRES("Créanciers hypothécaires"),
	PRESTATIONS_PREVOYANCE("Prestations de prévoyance"),
	REGULIERS("Réguliers"),
	LOI_TRAVAIL_AU_NOIR("Loi sur le travail au noir"),
	PARTICIPATIONS_HORS_SUISSE("Participations hors-Suisse"),
	EFFEUILLEUSES("Effeuilleuses");

	private final String texte;

	private CategorieImpotSource(String texte) {
		this.texte = texte;
	}

	public String texte() {
		return texte;
	}

}
