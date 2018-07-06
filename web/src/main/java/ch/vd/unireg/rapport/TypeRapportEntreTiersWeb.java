/**
 *
 */
package ch.vd.unireg.rapport;

import ch.vd.unireg.type.TypeRapportEntreTiers;

public enum TypeRapportEntreTiersWeb {
	TUTELLE,
	CURATELLE,
	CONSEIL_LEGAL,
	PRESTATION_IMPOSABLE,
	APPARTENANCE_MENAGE,
	REPRESENTATION,
	PARENTE,
	CONTACT_IMPOT_SOURCE,
	ANNULE_ET_REMPLACE,
	ASSUJETTISSEMENT_PAR_SUBSTITUTION,
	ACTIVITE_ECONOMIQUE,
	MANDAT,
	FUSION_ENTREPRISES,
	SCISSION_ENTREPRISE,
	TRANSFERT_PATRIMOINE,
	ADMINISTRATION_ENTREPRISE,
	SOCIETE_DIRECTION,
	HERITAGE,
	LIENS_ASSOCIES_ET_SNC;

	public static TypeRapportEntreTiersWeb fromCore(TypeRapportEntreTiers t) {
		return TypeRapportEntreTiersWeb.valueOf(t.name());
	}

	public TypeRapportEntreTiers toCore() {
		return TypeRapportEntreTiers.valueOf(name());
	}
}