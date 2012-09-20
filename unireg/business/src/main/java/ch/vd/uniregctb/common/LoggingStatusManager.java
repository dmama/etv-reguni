package ch.vd.uniregctb.common;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;


/**
 * Status manager qui n'interrompt jamais le processus et ne fait que logger les messages.
 */
public class LoggingStatusManager implements StatusManager {

	private final Logger logger;
	private final Level level;

	public LoggingStatusManager(Logger logger) {
		this.logger = logger;
		this.level = Level.INFO;
	}

	public LoggingStatusManager(Logger logger, Level level) {
		this.logger = logger;
		this.level = level;
	}

	@Override
	public boolean interrupted() {
		return false;
	}

	@Override
	public void setMessage(String msg) {
		this.logger.log(level, msg);
	}

	@Override
	public void setMessage(String msg, int percentProgression) {
		this.logger.log(level, msg + " (" + percentProgression + "%)");
	}
}