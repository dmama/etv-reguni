package ch.vd.uniregctb.type;


import org.apache.commons.lang.StringUtils;

public enum TypePermis {

	/**
	 * 01 (A) Permis Saisonnier
	 */
	SAISONNIER,

	/**
	 * 02 (B) Permis de séjour
	 */
	SEJOUR,

	/**
	 * 03 (C) Permis d'établissement
	 */
	ETABLISSEMENT,

	/**
	 * 04 (Ci) Conjoint/Enfant de diplomate ou de fonctionnaire international
	 */
	CONJOINT_DIPLOMATE,

	/**
	 * 05 (F) Étranger admis provisoirement
	 */
	ETRANGER_ADMIS_PROVISOIREMENT,

	/**
	 * 06 (G) Permis Frontalier
	 */
	FRONTALIER,

	/**
	 * 07 (L) Permis de séjour de courte durée
	 */
	COURTE_DUREE,

	/**
	 * 08 (N) Requérant d'asile
	 */
	REQUERANT_ASILE,

	/**
	 * 09 (S) Personne à protéger
	 */
	PERSONNE_A_PROTEGER,

	/**
	 * 10 Personne tenue de s'annoncer <br/>
	 * Pour les séjours tres court en Suisse de personnes qui doivent se déclarer.
	 */
	PERSONNE_TENUE_DE_S_ANNONCER,

	/**
	 * 11 Diplomate ou fonctionnaire internationnal avec immunité diplomatique
	 */
	DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE,

	/**
	 * 12 Fonctionnaire internationnal sans immunité diplomatique
	 */
	FONCT_INTER_SANS_IMMUNITE,

	/**
	 * 13 Pas Attribué
	 */
	PAS_ATTRIBUE,

	/**
	 * PAS DANS eVD - Statut provisoire (SP).<br/>
	 * Il s'agit d'un permis temporaire délivré à l'arrivée d'un étranger en attente de la décision de l'administration sur le permis accordé.
	 */
	PROVISOIRE,

	/**
	 * PAS DANS eVD - Suisse imposé à la source résidant à l'étranger (CH).
	 */
	SUISSE_SOURCIER;

	public static TypePermis getFromEvd(String evdPermisCode) {
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
			return SEJOUR;
		case 3:
			// permis C
			return ETABLISSEMENT;
		case 4:
			// permis Ci (fonctionnaire international ou conjoint/enfant de diplomate)
			return CONJOINT_DIPLOMATE;
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
		case 10:
			return PERSONNE_TENUE_DE_S_ANNONCER;
		case 11:
			return DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE;
		case 12:
			return FONCT_INTER_SANS_IMMUNITE;
		case 13:
			return PAS_ATTRIBUE;
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
		case SEJOUR:
			return "02";
		case ETABLISSEMENT:
			return "03";
		case CONJOINT_DIPLOMATE:
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
		case PERSONNE_TENUE_DE_S_ANNONCER:
			return "10";
		case DIPLOMATE_OU_FONCT_INTER_AVEC_IMMUNITE:
			return "11";
		case FONCT_INTER_SANS_IMMUNITE:
			return "12";
		case PAS_ATTRIBUE:
			return "13";
		case PROVISOIRE:
			return null;
		case SUISSE_SOURCIER:
			return null;
		default:
			throw new IllegalArgumentException("Type de permis inconnu = [" + typePermis + "]");

		}
	}
}
