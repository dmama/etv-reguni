/**
 *
 */
package ch.vd.uniregctb.type;

/**
 * Genre de permis réglementant le séjour d'une personne étrangère en Suisse.
 * Voir eCH-0006 pour les valeurs possibles
 */
public enum CategorieEtranger {

	_01_SAISONNIER_A,
	_02_PERMIS_SEJOUR_B,
	_03_ETABLI_C,
	_04_CONJOINT_DIPLOMATE_CI,
	_05_ETRANGER_ADMIS_PROVISOIREMENT_F,
	_06_FRONTALIER_G,
	_07_PERMIS_SEJOUR_COURTE_DUREE_L,
	_08_REQUERANT_ASILE_N,
	_09_A_PROTEGER_S,
	_10_TENUE_DE_S_ANNONCER,
	_11_DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE,
	_12_FONCT_INTER_SANS_IMMUNITE,
	_13_NON_ATTRIBUEE;
	
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