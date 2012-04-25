package ch.vd.uniregctb.common;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Ce logger permet de forcer le logging d'un message dans le niveau voulu, sans tenir compte du niveau de verbosité du logger lui-même.
 */
public class ForceLogger {

	private final Logger logger;

	public ForceLogger(Logger logger) {
		this.logger = logger;
	}

	public void trace(String message) {
		log(Level.TRACE, message, null);
	}

	public void debug(String message) {
		log(Level.DEBUG, message, null);
	}

	public void info(String message) {
		log(Level.INFO, message, null);
	}

	public void warn(String message) {
		log(Level.WARN, message, null);
	}

	private void log(Level level, String message, @Nullable Throwable t) {
		logger.callAppenders(new LoggingEvent(ForceLogger.class.getName(), logger, level, message, t));
	}
}