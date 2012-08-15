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
	_04_CONJOINT_DIPLOMATE_OU_FONCT_INT_CI,
	_05_ETRANGER_ADMIS_PROVISOIREMENT_F,
	_06_FRONTALIER_G,
	_07_PERMIS_SEJOUR_COURTE_DUREE_L,
	_08_REQUERANT_ASILE_N,
	_09_A_PROTEGER_S,
	_10_TENUE_DE_S_ANNONCER,
	_11_DIPLOMATE_OU_FONCTIONNAIRE_INTERNATIONAL,
	_13_NON_ATTRIBUEE;
	
	public static CategorieEtranger enumToCategorie(TypePermis permis) {
		if (permis == null)
			return null;
		
		if (TypePermis.ANNUEL == permis) {
			return _02_PERMIS_SEJOUR_B;
		}
		else if (TypePermis.ETABLISSEMENT == permis) {
			return _03_ETABLI_C;
		}
		else if (TypePermis.FRONTALIER == permis) {
			return _06_FRONTALIER_G;
		}
		else if (TypePermis.SUISSE_SOURCIER == permis) {
			return null;
		}
		else if (TypePermis.PROVISOIRE == permis) {
			return _05_ETRANGER_ADMIS_PROVISOIREMENT_F;
		}
		else if (TypePermis.REQUERANT_ASILE == permis) {
			return _08_REQUERANT_ASILE_N;
		}
		else if (TypePermis.ETRANGER_ADMIS_PROVISOIREMENT == permis) {
			return _05_ETRANGER_ADMIS_PROVISOIREMENT_F;
		}
		else if (TypePermis.COURTE_DUREE == permis) {
			return _07_PERMIS_SEJOUR_COURTE_DUREE_L;
		}
		else if (TypePermis.DIPLOMATE_OU_FONCTIONNAIRE_INTERNATIONAL == permis) {
			return _11_DIPLOMATE_OU_FONCTIONNAIRE_INTERNATIONAL;
		}
		else if (TypePermis.CONJOINT_DIPLOMATE_OU_FONCTIONNAIRE_INTERNATIONAL == permis) {
			return _04_CONJOINT_DIPLOMATE_OU_FONCT_INT_CI;
		}
		else if (TypePermis.PERSONNE_A_PROTEGER == permis) {
			return _09_A_PROTEGER_S;
		}
		else {
			throw new IllegalArgumentException("Type de permis inconnu = [" + permis + ']');
		}
	}
}