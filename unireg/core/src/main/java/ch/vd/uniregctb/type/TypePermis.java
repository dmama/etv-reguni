package ch.vd.uniregctb.type;

import ch.vd.registre.civil.model.EnumTypePermis;

public enum TypePermis {

	ANNUEL,
	COURTE_DUREE,
	DIPLOMATE,
	ETABLISSEMENT,
	FONCTIONNAIRE_INTERNATIONAL,
	FRONTALIER,
	/**
	 * Personne à protéger (dans le domaine de l'asile).
	 */
	PERSONNE_A_PROTEGER,
	PROVISOIRE,
	REQUERANT_ASILE_AVANT_DECISION,
	/**
	 * Requérant d'asile refusé en attente de rapatriement.
	 */
	REQUERANT_ASILE_REFUSE,
	/**
	 * Suisse imposé à la source résidant à l'étranger.
	 */
	SUISSE_SOURCIER;

	public static TypePermis get(EnumTypePermis right) {
		if (right == null) {
			return null;
		}
		if (right == EnumTypePermis.ANNUEL) {
			return ANNUEL;
		}
		else if (right == EnumTypePermis.COURTE_DUREE) {
			return COURTE_DUREE;
		}
		else if (right == EnumTypePermis.DIPLOMATE) {
			return DIPLOMATE;
		}
		else if (right == EnumTypePermis.ETABLLISSEMENT) {
			return ETABLISSEMENT;
		}
		else if (right == EnumTypePermis.FONCTIONNAIRE_INTERNATIONAL) {
			return FONCTIONNAIRE_INTERNATIONAL;
		}
		else if (right == EnumTypePermis.FRONTALIER) {
			return FRONTALIER;
		}
		else if (right == EnumTypePermis.PERSONNE_A_PROTEGER) {
			return PERSONNE_A_PROTEGER;
		}
		else if (right == EnumTypePermis.PROVISOIRE) {
			return PROVISOIRE;
		}
		else if (right == EnumTypePermis.REQUERANT_ASILE_AVANT_DECISION) {
			return REQUERANT_ASILE_AVANT_DECISION;
		}
		else if (right == EnumTypePermis.REQUERANT_ASILE_REFUSE) {
			return REQUERANT_ASILE_REFUSE;
		}
		else if (right == EnumTypePermis.SUISSE_SOURCIER) {
			return SUISSE_SOURCIER;
		}
		else {
			throw new IllegalArgumentException("Type de permis inconnu  = [" + right.getName() + "]");
		}
	}
}
