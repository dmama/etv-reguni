package ch.vd.uniregctb.migration.pm.log;

/**
 * Résultat de l'instanciation (= la résolution) d'un {@link LoggedElement}
 */
public class LoggedMessage {

	private final LogLevel level;
	private final String message;

	public LoggedMessage(LogLevel level, String message) {
		this.level = level;
		this.message = message;
	}

	public LogLevel getLevel() {
		return level;
	}

	public String getMessage() {
		return message;
	}
}
