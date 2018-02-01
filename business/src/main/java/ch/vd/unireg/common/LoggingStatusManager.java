package ch.vd.unireg.common;

import org.slf4j.Logger;

import ch.vd.unireg.utils.LogLevel;

/**
 * Status manager qui n'interrompt jamais le processus et ne fait que logger les messages.
 */
public class LoggingStatusManager implements StatusManager {

	private final Logger logger;
	private final LogLevel.Level level;

	public LoggingStatusManager(Logger logger) {
		this.logger = logger;
		this.level = LogLevel.Level.INFO;
	}

	public LoggingStatusManager(Logger logger, LogLevel.Level level) {
		this.logger = logger;
		this.level = level;
	}

	@Override
	public boolean isInterrupted() {
		return false;
	}

	@Override
	public void setMessage(String msg) {
		LogLevel.log(this.logger, level, msg);
	}

	@Override
	public void setMessage(String msg, int percentProgression) {
		LogLevel.log(this.logger, level, msg + " (" + percentProgression + "%)");
	}
}