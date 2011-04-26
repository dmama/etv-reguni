package ch.vd.uniregctb.general.view;

/**
 * Type de tiers (utilisé pour afficher une image)
 */
public enum TypeAvatar {
	HOMME,
	FEMME,
	SEXE_INCONNU,
	/** Ménage commun mixte */
	MC_MIXTE,
	/** Ménage commun avec homme seul (marié seul ou sexe conjoint inconnu) */
	MC_HOMME_SEUL,
	/** Ménage commun avec femme seule (mariée seule ou sexe conjoint inconnu) */
	MC_FEMME_SEULE,
	/** Ménage commun avec deux hommes (pacs) */
	MC_HOMME_HOMME,
	/** Ménage commun avec deux femmes (pacs) */
	MC_FEMME_FEMME,
	/** Ménage commun sans information de sexe */
	MC_SEXE_INCONNU,
	ENTREPRISE,
	ETABLISSEMENT,
	AUTRE_COMM,
	COLLECT_ADMIN,
	DEBITEUR
}
