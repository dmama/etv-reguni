package ch.vd.unireg.adresse;

import ch.vd.unireg.type.TypeAdresseTiers;

/**
 * Type d'adresse disponible au niveau fiscal.
 */
public enum TypeAdresseFiscale {

	COURRIER(TypeAdresseTiers.COURRIER),
	REPRESENTATION(TypeAdresseTiers.REPRESENTATION),
	POURSUITE(TypeAdresseTiers.POURSUITE),
	DOMICILE(TypeAdresseTiers.DOMICILE),
	POURSUITE_AUTRE_TIERS(null);

	private final TypeAdresseTiers coreType;

	TypeAdresseFiscale(TypeAdresseTiers coreType) {
		this.coreType = coreType;
	}

	public TypeAdresseTiers asCoreType() {
		return coreType;
	}

	public static TypeAdresseFiscale fromCore(TypeAdresseTiers type) {
		if (type == null) {
			return null;
		}

		switch (type) {
		case COURRIER:
			return COURRIER;
		case REPRESENTATION:
			return REPRESENTATION;
		case DOMICILE:
			return DOMICILE;
		case POURSUITE:
			return POURSUITE;
		default:
			throw new IllegalArgumentException("Type d'adresse tiers inconnu = [" + type + ']');
		}
	}
}
