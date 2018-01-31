package ch.vd.uniregctb.registrefoncier;

/**
 * Type de communauté (source de la traduction : http://www.grundbuchverwalter.ch/fr/downloads/send/9-office-federal-charge-du-droit-du-registre-foncier-et-du-droit-foncier-ofrf-projet-federal-egris/67-modele-de-donnees-conceptuel-egris)
 */
public enum TypeCommunaute {
	SOCIETE_SIMPLE,         // EinfacheGesellschaft
	COMMUNAUTE_HEREDITAIRE, // Erbengemeinschaft
	COMMUNAUTE_DE_BIENS,    // Guetergemeinschaft
	INDIVISION,             // Gemeinderschaft
	INCONNU
}
