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
		// selon la documentation eCH-0006 (http://www.ech.ch/alfresco/guestDownload/attach/workspace/SpacesStore/55bbdceb-5696-4dbe-8b5c-e12ad073c49f/STAN_f_DEF_2005-11-02_eCH-0006%20Ausl%C3%A4nderkategorien.pdf),
		// la catégorie de permis est une chaîne de caractères "XXYYZZ" découpée en trois groupes :
		//   - XX : catégorie d'étranger (par exemple, permis L)
		//   - YY : réglement en action (par exemple, accord UE/AELE)
		//   - ZZ : précisions réservées à certaines catégories d'étranger
		// Dès le 1er janvier 2013, tous les permis devront avec les deux premiers groupes renseignés, mais en attendant, certains permis
		// n'ont que le premier groupe renseigné.

		// voir aussi la spécification RCPers "TEC-CatalogueOfficielCaracteres.doc"

		final int categorie = Integer.parseInt(evdPermisCode.substring(0, 2));
		switch (categorie) {
		case 1:
			// permis A, pas d'équivalence
			return null;
		case 2:
			// permis B
			return ANNUEL;
		case 3:
		case 4:
			// permis C
			return ETABLISSEMENT;
		case 5:
			// permis F
			return PROVISOIRE;
		case 6:
			// permis G
			return FRONTALIER;
		case 7:
			// permis L
			return COURTE_DUREE;
		case 8:
			// permis N
			return REQUERANT_ASILE_AVANT_DECISION;
		case 9:
			// permis S
			return PERSONNE_A_PROTEGER;
		case 11:
			return DIPLOMATE;
		case 12:
			return FONCTIONNAIRE_INTERNATIONAL;
		default:
			// hors-catégorie
			return null;
		}
	}
}
