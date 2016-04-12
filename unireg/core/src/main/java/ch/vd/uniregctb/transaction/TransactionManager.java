package ch.vd.uniregctb.transaction;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;

import org.apache.geronimo.transaction.manager.TransactionLog;
import org.apache.geronimo.transaction.manager.XidFactory;
import org.jencks.GeronimoPlatformTransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.uniregctb.utils.LogLevel;

/**
 * Classe de tracing des temps passés dans les méthode de l'interface {@link javax.transaction.TransactionManager}
 */
public class TransactionManager extends GeronimoPlatformTransactionManager {

	/**
	 * Logger utilisé pour les temps d'accès mesurés
	 * <ul>
	 * <li>commit et rollback sont loggués en INFO</li>
	 * <li>begin, suspend et resume sont loggués en DEBUG</li>
	 * <li>tous sont loggués en WARN si le temps est plus grand que {@link #WARNING_THRESHOLD}
	 * </ul> 
	 */
	public static final Logger LOGGER = LoggerFactory.getLogger(TransactionManager.class);

	/**
	 * Temps de réponse (en ns) au delà duquel on passe de toute façon en niveau WARN
	 */
	private static final long WARNING_THRESHOLD = 50000000L;

	public TransactionManager(int defaultTransactionTimeoutSeconds, XidFactory xidFactory, TransactionLog transactionLog) throws XAException {
		super(defaultTransactionTimeoutSeconds, xidFactory, transactionLog);
	}

	private static long start() {
		return System.nanoTime();
	}

	private static void end(long start, String name, LogLevel.Level loggingLevel, long threshold, LogLevel.Level aboveThresholdLevel) {
		final long end = System.nanoTime();
		final long duration = end - start;
		final LogLevel.Level effectiveLevel = (duration >= threshold ? aboveThresholdLevel : loggingLevel);
		if (LogLevel.isEnabledFor(LOGGER, effectiveLevel)) {
			LogLevel.log(LOGGER, effectiveLevel, String.format("(%d ms) %s", duration / 1000000L, name));
		}
	}

	@Override
	public void commit() throws HeuristicMixedException, HeuristicRollbackException, IllegalStateException, RollbackException, SecurityException, SystemException {
		final long start = start();
		try {
			super.commit();
		}
		finally {
			end(start, "commit", LogLevel.Level.INFO, WARNING_THRESHOLD, LogLevel.Level.WARN);
		}
	}

	@Override
	public void rollback() throws IllegalStateException, SecurityException, SystemException {
		final long start = start();
		try {
			super.rollback();
		}
		finally {
			end(start, "rollback", LogLevel.Level.INFO, WARNING_THRESHOLD, LogLevel.Level.WARN);
		}
	}

	@Override
	public void begin() throws NotSupportedException, SystemException {
		final long start = start();
		try {
			super.begin();
		}
		finally {
			end(start, "begin", LogLevel.Level.DEBUG, WARNING_THRESHOLD, LogLevel.Level.WARN);
		}
	}

	@Override
	public Transaction suspend() throws SystemException {
		final long start = start();
		try {
			return super.suspend();
		}
		finally {
			end(start, "suspend", LogLevel.Level.DEBUG, WARNING_THRESHOLD, LogLevel.Level.WARN);
		}
	}

	@Override
	public void resume(Transaction tx) throws IllegalStateException, InvalidTransactionException, SystemException {
		final long start = start();
		try {
			super.resume(tx);
		}
		finally {
			end(start, "resume", LogLevel.Level.DEBUG, WARNING_THRESHOLD, LogLevel.Level.WARN);
		}
	}

	@Override
	public void setRollbackOnly() throws IllegalStateException {
		// don't do anything at this stage... otherwise the rollback cause will be lost in the transaction itself
	}
}
