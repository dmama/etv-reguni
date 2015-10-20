package ch.vd.uniregctb.migration.pm;

/**
 *  les phases de consolidations des données à la fin d'une transaction
 *  (les consolidations seront lancées dans l'ordre des constantes du type énuméré)
 */
public enum ConsolidationPhase {

	/**
	 * Le calcul des fors secondaires "activité"
	 */
	FORS_ACTIVITE,

	/**
	 * Le calcul des fors secondaires "immeuble"
	 */
	FORS_IMMEUBLES,

	/**
	 * Le contrôle des fors secondaires avant/après migration (doit être après {@link #FORS_ACTIVITE} et {@link #FORS_IMMEUBLES})
	 */
	CONTROLE_FORS_SECONDAIRES,

	/**
	 * Le contrôle et mise à jour des fors principaux en fonction des fors secondaires (il y a parfois des zones non-couvertes de fors secondaires)
	 */
	COUVERTURE_FORS,

	/**
	 * Le nettoyage des fors fiscaux déjà annulés (lors des éventuelles modifications intervenue durant la migration)
	 */
	EFFACEMENT_FORS_ANNULES,

	/**
	 * Le recalcul des motifs des fors principaux encore {@link ch.vd.uniregctb.type.MotifFor#INDETERMINE indéterminés}
	 */
	RECALCUL_MOTIFS_INDETERMINES,

	/**
	 * Le calcul des dates des déclarations d'impôt qui ne sont pas associées dans RegPM à un exercice commercial (= elle n'ont pas encore été retournées)
	 * et dont on veut estimer les dates d'après les périodes d'imposition calculées
	 */
	DECLARATIONS_SANS_EXERCICE_COMMERCIAL_REGPM,

	/**
	 * L'annulation des données (fors, déclarations...) des contribuables inactifs (doublons) ou qui n'auraient pas d'assujettissement dans RegPM (communes...)
	 */
	ANNULATION_DONNEES_CONTRIBUABLES_INACTIFS,

	/**
	 * Le contrôle des assujettissements avant/après la migration
	 */
	COMPARAISON_ASSUJETTISSEMENTS
}
