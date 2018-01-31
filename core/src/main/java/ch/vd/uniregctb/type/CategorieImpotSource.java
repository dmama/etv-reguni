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
public enum CategorieImpotSource implements RestrictedAccess {

	ADMINISTRATEURS("Administrateurs"),
	CONFERENCIERS_ARTISTES_SPORTIFS("Conférenciers, artistes, sportifs"),
	CREANCIERS_HYPOTHECAIRES("Créanciers hypothécaires"),
	PRESTATIONS_PREVOYANCE("Prestations de prévoyance"),
	REGULIERS("Réguliers"),
	LOI_TRAVAIL_AU_NOIR("Loi sur le travail au noir"),
	/**
	 * @since 5.5.x - 13R3
	 */
	PARTICIPATIONS_HORS_SUISSE("Participations Hors-Suisse"),
	/**
	 * @since 5.5.x - 13R3 (5.7.x / 14R1 pour l'utilisabilité dans l'application)
	 */
	EFFEUILLEUSES("Saisonniers agricoles et viticoles");

	private final String texte;

	CategorieImpotSource(String texte) {
		this.texte = texte;
	}

	public String texte() {
		return texte;
	}

	public boolean isAllowed() {
		return true;
	}
}
