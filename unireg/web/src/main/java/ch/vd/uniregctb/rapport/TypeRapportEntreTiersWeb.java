/**
 *
 */
package ch.vd.uniregctb.rapport;

import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public enum TypeRapportEntreTiersWeb {
	TUTELLE,
	CURATELLE,
	CONSEIL_LEGAL,
	PRESTATION_IMPOSABLE,
	APPARTENANCE_MENAGE,
	REPRESENTATION,
	FILIATION,
	CONTACT_IMPOT_SOURCE,
	ANNULE_ET_REMPLACE;

	public static TypeRapportEntreTiersWeb fromCore(TypeRapportEntreTiers t) {
		return TypeRapportEntreTiersWeb.valueOf(t.name());
	}

	public TypeRapportEntreTiers toCore() {
		return TypeRapportEntreTiers.valueOf(name());
	}
}