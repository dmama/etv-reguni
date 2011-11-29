package ch.vd.uniregctb.type;


import org.apache.commons.lang.StringUtils;

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
			throw new IllegalArgumentException("Type de permis inconnu  = [" + right.getName() + ']');
		}
	}

	public static TypePermis get(String evdPermisCode) {
		if (StringUtils.isBlank(evdPermisCode)) {
			return null;
		}
		// voir spécification RCPers "TEC-CatalogueOfficielCaracteres.doc"
		final int code = Integer.parseInt(evdPermisCode.substring(0,4));
		if (100 >= code && code < 200) {
			// permis A, pas d'équivalence
			return null;
		}
		else if (200 >= code && code < 300) {
			// permis B
			return ANNUEL;
		}
		else if (300 >= code && code < 500) {
			// permis C
			return ETABLISSEMENT;
		}
		else if (500 >= code && code < 600) {
			// permis F
			return PROVISOIRE;
		}
		else if (600 >= code && code < 700) {
			// permis G
			return FRONTALIER;
		}
		else if (700 >= code && code < 800) {
			// permis L
			return COURTE_DUREE;
		}
		else if (800 >= code && code < 900) {
			// permis N
			return REQUERANT_ASILE_AVANT_DECISION;
		}
		else if (900 >= code && code < 1000) {
			// permis S
			return PERSONNE_A_PROTEGER;
		}
		else if (code == 1107) {
			return DIPLOMATE;
		}
		else if (code == 1208) {
			return FONCTIONNAIRE_INTERNATIONAL;
		}
		else {
			// hors-catégorie
			return null;
		}
	}
}
