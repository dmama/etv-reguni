package ch.vd.uniregctb.migration.pm;

public abstract class MigrationConstants {

	/**
	 * Le visa utilisé par la migration dans Unireg
	 */
	public static final String VISA_MIGRATION = "[MigrationPM]";

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

	/**
	 * Le contrôle et mise à jour des fors principaux en fonction des fors secondaires (il y a parfois des zones non-couvertes de fors secondaires)
	 */
	public static final int PHASE_COUVERTURE_FORS = 40;
}
