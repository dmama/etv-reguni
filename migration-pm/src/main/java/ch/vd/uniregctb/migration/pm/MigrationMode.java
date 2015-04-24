package ch.vd.uniregctb.migration.pm;

public enum MigrationMode {

	/**
	 * Prend les données du mainframe, et les sauvegarde dans des fichiers locaux (= pas de migration !), utilisé en tests
	 */
	DUMP,

	/**
	 * Part des fichiers locaux (voir mode {@link #DUMP}) et lance la migration, utilisé en tests
	 */
	FROM_DUMP,

	/**
	 * Prend les données du mainframe et lance la migration sans passer par des fichiers intermédiaires (= cible en production)
	 */
	DIRECT
}
