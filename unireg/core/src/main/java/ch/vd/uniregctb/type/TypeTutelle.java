package ch.vd.uniregctb.type;

import ch.vd.registre.civil.model.EnumTypeTutelle;

/**
 * Modélise les différents types de tutelle.
 *
 * @author Ludovic BERTIN
 */
public enum TypeTutelle {
	TUTELLE,
	CURATELLE,
	CONSEIL_LEGAL;

	public static TypeTutelle get(EnumTypeTutelle right) {
		if (right == null) {
			return null;
		}
		else if (right == EnumTypeTutelle.TUTELLE) {
			return TUTELLE;
		}
		else if (right == EnumTypeTutelle.CURATELLE) {
			return CURATELLE;
		}
		else if (right == EnumTypeTutelle.CONSEIL_LEGAL) {
			return CONSEIL_LEGAL;
		}
		else {
			throw new IllegalArgumentException("Type de tutelle inconnu = [" + right.getName() + ']');
		}
	}
}
