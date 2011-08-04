package ch.vd.uniregctb.audit;

import org.apache.log4j.Level;
/*
 * Longueur de colonne : 14
 */
public enum AuditLevel {

	SUCCESS(Level.INFO),
	INFO(Level.INFO),
	WARN(Level.WARN),
	ERROR(Level.ERROR);

	private final Level log4jLevel;

	AuditLevel(Level log4jLevel) {
		this.log4jLevel = log4jLevel;
	}

	public Level asLog4j() {
		return log4jLevel;
	}
}
