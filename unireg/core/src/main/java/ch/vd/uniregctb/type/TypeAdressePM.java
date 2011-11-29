/**
 *
 */
package ch.vd.uniregctb.type;

import ch.vd.registre.pm.model.EnumTypeAdresseEntreprise;

/**
 * Longueur de colonne : 11
 */
public enum TypeAdressePM {
	COURRIER,
	SIEGE,
	FACTURATION;

	public static TypeAdressePM get(EnumTypeAdresseEntreprise right) {
		if (right == null) {
			return null;
		}
		if (right == EnumTypeAdresseEntreprise.COURRIER) {
			return COURRIER;
		}
		else if (right == EnumTypeAdresseEntreprise.SIEGE) {
			return SIEGE;
		}
		else if (right == EnumTypeAdresseEntreprise.FACTURATION) {
			return FACTURATION;
		}
		else {
			throw new IllegalArgumentException("Type d'adresse PM inconnu = [" + right.getName() + ']');
		}
	}
}