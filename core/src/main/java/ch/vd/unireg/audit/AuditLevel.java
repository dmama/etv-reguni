package ch.vd.unireg.audit;

import ch.vd.unireg.utils.LogLevel;

/*
 * Longueur de colonne : 14
 */
public enum AuditLevel {

	SUCCESS(LogLevel.Level.INFO),
	INFO(LogLevel.Level.INFO),
	WARN(LogLevel.Level.WARN),
	ERROR(LogLevel.Level.ERROR);

	private final LogLevel.Level logLevel;

	AuditLevel(LogLevel.Level logLevel) {
		this.logLevel = logLevel;
	}

	public LogLevel.Level asLogLevel() {
		return logLevel;
	}
}
