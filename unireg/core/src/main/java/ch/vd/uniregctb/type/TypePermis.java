package ch.vd.uniregctb.type;


import org.apache.commons.lang.StringUtils;

public enum TypePermis {
	/**
	 * Saisonnier (A). Ce permis a été supprimé en mai 2002.
	 */
	SAISONNIER,
	/**
	 * Titulaire d’un permis de séjour (B)
	 */
	ANNUEL,
	/**
	 * Titulaire d’un permis de séjour de courte durée (L)
	 */
	COURTE_DUREE,
	/**
	 * Diplomate (D ou code LHR 11xx)
	 */
	DIPLOMATE,
	/**
	 * Titulaire d'un permis d'établissement (C)
	 */
	ETABLISSEMENT,
	/**
	 * Fonctionnaire internal ou conjoint/enfant de diplomate (Ci)
	 */
	FONCTIONNAIRE_INTERNATIONAL,
	/**
	 * Frontalier (G)
	 */
	FRONTALIER,
	/**
	 * Personne à protéger (S).
	 */
	PERSONNE_A_PROTEGER,
	/**
	 * Statut provisoire (SP). Il s'agit d'un permis temporaire délivré à l'arrivée d'un étranger en attente de la décision de l'administration sur le permis accordé.
	 */
	PROVISOIRE,
	/**
	 * Requérant d'asile (N)
	 */
	REQUERANT_ASILE,
	/**
	 * Étranger admis provisoirement (F)
	 */
	ETRANGER_ADMIS_PROVISOIREMENT,
	/**
	 * Suisse imposé à la source résidant à l'étranger (CH).
	 */
	SUISSE_SOURCIER;

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
			// permis A
			return SAISONNIER;
		case 2:
			// permis B
			return ANNUEL;
		case 3:
			// permis C
			return ETABLISSEMENT;
		case 4:
			// permis Ci (fonctionnaire international ou conjoint/enfant de diplomate)
			return FONCTIONNAIRE_INTERNATIONAL;
		case 5:
			// permis F
			return ETRANGER_ADMIS_PROVISOIREMENT;
		case 6:
			// permis G
			return FRONTALIER;
		case 7:
			// permis L
			return COURTE_DUREE;
		case 8:
			// permis N
			return REQUERANT_ASILE;
		case 9:
			// permis S
			return PERSONNE_A_PROTEGER;
		case 11:
			return DIPLOMATE;
		case 12:
			return FONCTIONNAIRE_INTERNATIONAL;
		case 13:
			return PROVISOIRE;
		default:
			// hors-catégorie
			return null;
		}
	}

	public static String toEch(TypePermis typePermis) {
		if (typePermis == null) {
			return null;
		}

		switch (typePermis) {
		case SAISONNIER:
			return "01";
		case ANNUEL:
			return "02";
		case ETABLISSEMENT:
			return "03";
		case FONCTIONNAIRE_INTERNATIONAL:
			return "04";
		case ETRANGER_ADMIS_PROVISOIREMENT:
			return "05";
		case FRONTALIER:
			return "06";
		case COURTE_DUREE:
			return "07";
		case REQUERANT_ASILE:
			return "08";
		case PERSONNE_A_PROTEGER:
			return "09";
		case DIPLOMATE:
			return "11";
		case PROVISOIRE:
			return "13";
		case SUISSE_SOURCIER:
			return null;
		default:
			throw new IllegalArgumentException("Type de permis inconnu = [" + typePermis + "]");

		}
	}
}
