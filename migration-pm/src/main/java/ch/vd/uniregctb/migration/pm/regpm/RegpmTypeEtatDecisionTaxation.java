package ch.vd.uniregctb.migration.pm.regpm;

/**
 * L'état d'une décision de taxation PM
 */
public enum RegpmTypeEtatDecisionTaxation {
	NON_NOTIFIEE,
	NOTIFIEE,
	ENTREE_EN_FORCE,
	A_REVISER,
	EN_RECLAMATION,
	ERREUR_DE_CALCUL,
	ERREUR_DE_TRANSCRIPTION,
	ARTICLE_98_120,
	ANNULEE,
	EN_SOUSTRACTION
}
