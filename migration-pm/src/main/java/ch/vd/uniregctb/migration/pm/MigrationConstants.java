package ch.vd.uniregctb.migration.pm;

public abstract class MigrationConstants {

	//
	// les phases de consolidations des données à la fin d'une transaction
	// (les consolidations seront lancées dans l'ordre croissant du numéro de phase)
	//

	/**
	 * Le calcul des fors secondaires "activité"
	 */
	public static final int PHASE_FORS_ACTIVITE = 10;

	/**
	 * Le calcul des fors secondaires "immeuble"
	 */
	public static final int PHASE_FORS_IMMEUBLES = 20;

	/**
	 * Le contrôle des fors secondaires avant/après migration (doit être après {@link #PHASE_FORS_ACTIVITE} et {@link #PHASE_FORS_IMMEUBLES})
	 */
	public static final int PHASE_CONTROLE_FORS_SECONDAIRES = 30;
}
