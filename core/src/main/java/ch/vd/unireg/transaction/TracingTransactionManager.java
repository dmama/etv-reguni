package ch.vd.unireg.transaction;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.utils.LogLevel;

/**
 * Classe de tracing des temps passés dans les méthode de l'interface {@link javax.transaction.TransactionManager}
 */
public class TracingTransactionManager implements UserTransaction, TransactionManager {

	private TransactionManager target;

	/**
	 * Logger utilisé pour les temps d'accès mesurés
	 * <ul>
	 * <li>commit et rollback sont loggués en INFO</li>
	 * <li>begin, suspend et resume sont loggués en DEBUG</li>
	 * <li>tous sont loggués en WARN si le temps est plus grand que {@link #WARNING_THRESHOLD}
	 * </ul>
	 */
	public static final Logger LOGGER = LoggerFactory.getLogger(TracingTransactionManager.class);

	/**
	 * Temps de réponse (en ns) au delà duquel on passe de toute façon en niveau WARN
	 */
	private static final long WARNING_THRESHOLD = TimeUnit.MILLISECONDS.toNanos(50);

	private static long start() {
		return System.nanoTime();
	}

	private static void end(long start, String name, LogLevel.Level loggingLevel, long threshold, LogLevel.Level aboveThresholdLevel) {
		final long end = System.nanoTime();
		final long duration = end - start;
		final LogLevel.Level effectiveLevel = (duration >= threshold ? aboveThresholdLevel : loggingLevel);
		if (LogLevel.isEnabledFor(LOGGER, effectiveLevel)) {
			LogLevel.log(LOGGER, effectiveLevel, String.format("(%d ms) %s", TimeUnit.NANOSECONDS.toMillis(duration), name));
		}
	}

	public void setTarget(TransactionManager target) {
		this.target = target;
	}

	@Override
	public void begin() throws NotSupportedException, SystemException {
		final long start = start();
		try {
			target.begin();
		}
		finally {
			end(start, "begin", LogLevel.Level.DEBUG, WARNING_THRESHOLD, LogLevel.Level.WARN);
		}
	}

	@Override
	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
		final long start = start();
		try {
			target.commit();
		}
		finally {
			end(start, "commit", LogLevel.Level.INFO, WARNING_THRESHOLD, LogLevel.Level.WARN);
		}
	}

	@Override
	public void rollback() throws IllegalStateException, SecurityException, SystemException {
		final long start = start();
		try {
			target.rollback();
		}
		finally {
			end(start, "rollback", LogLevel.Level.INFO, WARNING_THRESHOLD, LogLevel.Level.WARN);
		}
	}

	@Override
	public void resume(Transaction tobj) throws InvalidTransactionException, IllegalStateException, SystemException {
		final long start = start();
		try {
			target.resume(tobj);
		}
		finally {
			end(start, "resume", LogLevel.Level.DEBUG, WARNING_THRESHOLD, LogLevel.Level.WARN);
		}
	}

	@Override
	public Transaction suspend() throws SystemException {
		final long start = start();
		try {
			return target.suspend();
		}
		finally {
			end(start, "suspend", LogLevel.Level.DEBUG, WARNING_THRESHOLD, LogLevel.Level.WARN);
		}
	}

	@Override
	public Transaction getTransaction() throws SystemException {
		return target.getTransaction();
	}

	@Override
	public void setRollbackOnly() throws IllegalStateException, SystemException {
		target.setRollbackOnly();
	}

	@Override
	public int getStatus() throws SystemException {
		return target.getStatus();
	}

	@Override
	public void setTransactionTimeout(int seconds) throws SystemException {
		target.setTransactionTimeout(seconds);
	}

	@Override
	public String toString() {
		return String.format("%s on %s", getClass().getName(), target);
	}
}
