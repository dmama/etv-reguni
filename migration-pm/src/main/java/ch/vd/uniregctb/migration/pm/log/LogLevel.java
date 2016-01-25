package ch.vd.uniregctb.migration.pm.log;

/**
 * Les niveaux de log, dans l'ordre croissant de gravité
 */
public enum LogLevel {
	/**
	 * DEBUG, ne devrait pas être activé en production
	 */
	DEBUG,

	/**
	 * Message informatif de suivi de la migration
	 */
	INFO,

	/**
	 * Il y a quelque chose de louche, ou en tout cas à regarder avec attention
	 */
	WARN,

	/**
	 * Là, c'est sûr, il y a un souci...
	 */
	ERROR
}
