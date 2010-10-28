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
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jencks.GeronimoPlatformTransactionManager;

/**
 * Classe de tracing des temps passés dans les méthode de l'interface {@link javax.transaction.TransactionManager}
 */
public class TransactionManager extends GeronimoPlatformTransactionManager {

	/**
	 * Logger utilisé pour les temps d'accès mesurés
	 * <ul>
	 * <li>commit et rollback sont loggués en {@link org.apache.log4j.Level#INFO}</li>
	 * <li>begin, suspend et resume sont loggués en {@link org.apache.log4j.Level#DEBUG}</li>
	 * <li>tous sont loggués en {@link org.apache.log4j.Level#WARN} si le temps est plus grand que {@link #WARNING_THRESHOLD}
	 * </ul> 
	 */
	public static final Logger LOGGER = Logger.getLogger(TransactionManager.class);

	/**
	 * Temps de réponse (en ns) au delà duquel on passe de toute façon en niveau {@link org.apache.log4j.Level#WARN}
	 */
	private static final long WARNING_THRESHOLD = 50000000L;

	public TransactionManager(int defaultTransactionTimeoutSeconds, XidFactory xidFactory, TransactionLog transactionLog) throws XAException {
		super(defaultTransactionTimeoutSeconds, xidFactory, transactionLog);
	}

	private static long start() {
		return System.nanoTime();
	}

	private static void end(long start, String name, Level loggingLevel, long threshold, Level aboveThresholdLevel) {
		final long end = System.nanoTime();
		final long duration = end - start;
		final Level effectiveLevel = (duration >= threshold ? aboveThresholdLevel : loggingLevel);
		if (LOGGER.isEnabledFor(effectiveLevel)) {
			LOGGER.log(effectiveLevel, String.format("(%d ms) %s", duration / 1000000L, name));
		}
	}

	@Override
	public void commit() throws HeuristicMixedException, HeuristicRollbackException, IllegalStateException, RollbackException, SecurityException, SystemException {
		final long start = start();
		try {
			super.commit();
		}
		finally {
			end(start, "commit", Level.INFO, WARNING_THRESHOLD, Level.WARN);
		}
	}

	@Override
	public void rollback() throws IllegalStateException, SecurityException, SystemException {
		final long start = start();
		try {
			super.rollback();
		}
		finally {
			end(start, "rollback", Level.INFO, WARNING_THRESHOLD, Level.WARN);
		}
	}

	@Override
	public void begin() throws NotSupportedException, SystemException {
		final long start = start();
		try {
			super.begin();
		}
		finally {
			end(start, "begin", Level.DEBUG, WARNING_THRESHOLD, Level.WARN);
		}
	}

	@Override
	public Transaction suspend() throws SystemException {
		final long start = start();
		try {
			return super.suspend();
		}
		finally {
			end(start, "suspend", Level.DEBUG, WARNING_THRESHOLD, Level.WARN);
		}
	}

	@Override
	public void resume(Transaction tx) throws IllegalStateException, InvalidTransactionException, SystemException {
		final long start = start();
		try {
			super.resume(tx);
		}
		finally {
			end(start, "resume", Level.DEBUG, WARNING_THRESHOLD, Level.WARN);
		}
	}
}
