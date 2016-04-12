package ch.vd.uniregctb.migration.pm;

public abstract class MigrationConstants {

	/**
	 * Le visa utilisé par la migration dans Unireg
	 */
	public static final String VISA_MIGRATION = "[MigrationPM]";

	/**
	 * La valeur à mettre dans le champ "source" d'un état de DI retournée lors de la migration
	 */
	public static final String SOURCE_RETOUR_DI_MIGREE = "SDI";

	/**
	 * La valeur à mettre dans le champ "source" d'un état de questionnaire SNC retourné lors de la migration
	 */
	public static final String SOURCE_RETOUR_QSNC_MIGRE = "SDI";

	/**
	 * L'année de la toute première PF prise en compte dans la migration
	 */
	public static final int PREMIERE_PF = 1995;
}
