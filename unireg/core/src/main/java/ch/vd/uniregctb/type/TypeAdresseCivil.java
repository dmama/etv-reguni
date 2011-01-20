/**
 *
 */
package ch.vd.uniregctb.type;

import ch.vd.common.model.EnumTypeAdresse;

public enum TypeAdresseCivil {
	SECONDAIRE(EnumTypeAdresse.SECONDAIRE),
	PRINCIPALE(EnumTypeAdresse.PRINCIPALE),
	COURRIER(EnumTypeAdresse.COURRIER),
	TUTEUR(EnumTypeAdresse.TUTELLE);

	private EnumTypeAdresse host;

	TypeAdresseCivil(EnumTypeAdresse host) {
		this.host = host;
	}

	public static TypeAdresseCivil get(EnumTypeAdresse right) {
		if (right == null) {
			return null;
		}

		if (right == EnumTypeAdresse.SECONDAIRE) {
			return SECONDAIRE;
		}
		else if (right == EnumTypeAdresse.PRINCIPALE) {
			return PRINCIPALE;
		}
		else if (right == EnumTypeAdresse.COURRIER) {
			return COURRIER;
		}
		else if (right == EnumTypeAdresse.TUTELLE) {
			return TUTEUR;
		}
		else {
			throw new IllegalArgumentException("Type d'adresse civile inconnue = [" + right.getName() + "]");
		}
	}

	public EnumTypeAdresse asHost() {
		return host;
	}
}