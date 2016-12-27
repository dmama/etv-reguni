/**
 *
 */
package ch.vd.uniregctb.type;

/**
 * Genre de permis réglementant le séjour d'une personne étrangère en Suisse.
 * Voir eCH-0006 pour les valeurs possibles
 */
public enum CategorieEtranger {

	_01_SAISONNIER_A("Saisonnier (A)"),
	_02_PERMIS_SEJOUR_B("Séjour (B)"),
	_03_ETABLI_C("Etablissement (C)"),
	_04_CONJOINT_DIPLOMATE_CI("Conjoint de diplomate (Ci)"),
	_05_ETRANGER_ADMIS_PROVISOIREMENT_F("Etranger admis provisoirement (F)"),
	_06_FRONTALIER_G("Frontalier (G)"),
	_07_PERMIS_SEJOUR_COURTE_DUREE_L("Séjour de courte durée (L)"),
	_08_REQUERANT_ASILE_N("Requérant d'asile (N)"),
	_09_A_PROTEGER_S("A protéger (S)"),
	_10_TENUE_DE_S_ANNONCER("Tenu de s'annoncer"),
	_11_DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE("Diplomate ou fonctionnaire international"),
	_12_FONCT_INTER_SANS_IMMUNITE("Fonctionaire international sans immunité"),
	_13_NON_ATTRIBUEE("Non attribué");

	private final String displayName;

	CategorieEtranger(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static CategorieEtranger valueOf(TypePermis permis) {
		if (permis == null)
			return null;

		if (TypePermis.SAISONNIER == permis) {
			return _01_SAISONNIER_A;
		}
		if (TypePermis.SEJOUR == permis) {
			return _02_PERMIS_SEJOUR_B;
		}
		else if (TypePermis.ETABLISSEMENT == permis) {
			return _03_ETABLI_C;
		}
		else if (TypePermis.CONJOINT_DIPLOMATE == permis) {
			return _04_CONJOINT_DIPLOMATE_CI;
		}
		else if (TypePermis.ETRANGER_ADMIS_PROVISOIREMENT == permis) {
			return _05_ETRANGER_ADMIS_PROVISOIREMENT_F;
		}
		else if (TypePermis.FRONTALIER == permis) {
			return _06_FRONTALIER_G;
		}
		else if (TypePermis.COURTE_DUREE == permis) {
			return _07_PERMIS_SEJOUR_COURTE_DUREE_L;
		}
		else if (TypePermis.REQUERANT_ASILE == permis) {
			return _08_REQUERANT_ASILE_N;
		}
		else if (TypePermis.PERSONNE_A_PROTEGER == permis) {
			return _09_A_PROTEGER_S;
		}
		else if (TypePermis.PERSONNE_TENUE_DE_S_ANNONCER == permis) {
			return _10_TENUE_DE_S_ANNONCER;
		}
		else if (TypePermis.DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE == permis) {
			return _11_DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE;
		}
		else if (TypePermis.FONCT_INTER_SANS_IMMUNITE == permis) {
			return _12_FONCT_INTER_SANS_IMMUNITE;
		}
		else if (TypePermis.PAS_ATTRIBUE == permis) {
			return _13_NON_ATTRIBUEE;
		}
		else if (TypePermis.PROVISOIRE == permis) {
			return _13_NON_ATTRIBUEE;
		}
		else if (TypePermis.SUISSE_SOURCIER == permis) {
			return null;
		}
		else {
			throw new IllegalArgumentException("Type de permis inconnu = [" + permis + ']');
		}
	}
}