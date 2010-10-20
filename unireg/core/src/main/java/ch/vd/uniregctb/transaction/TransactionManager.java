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
	 * </ul> 
	 */
	public static final Logger LOGGER = Logger.getLogger(TransactionManager.class);

	public TransactionManager(int defaultTransactionTimeoutSeconds, XidFactory xidFactory, TransactionLog transactionLog) throws XAException {
		super(defaultTransactionTimeoutSeconds, xidFactory, transactionLog);
	}

	private static long start() {
		return System.nanoTime();
	}

	private static void end(long start, String name, Level loggingLevel) {
		final long end = System.nanoTime();
		if (LOGGER.isEnabledFor(loggingLevel)) {
			LOGGER.log(loggingLevel, String.format("(%d ms) %s", (end - start) / 1000000L, name));
		}
	}

	@Override
	public void commit() throws HeuristicMixedException, HeuristicRollbackException, IllegalStateException, RollbackException, SecurityException, SystemException {
		final long start = start();
		try {
			super.commit();
		}
		finally {
			end(start, "commit", Level.INFO);
		}
	}

	@Override
	public void rollback() throws IllegalStateException, SecurityException, SystemException {
		final long start = start();
		try {
			super.rollback();
		}
		finally {
			end(start, "rollback", Level.INFO);
		}
	}

	@Override
	public void begin() throws NotSupportedException, SystemException {
		final long start = start();
		try {
			super.begin();
		}
		finally {
			end(start, "begin", Level.DEBUG);
		}
	}

	@Override
	public Transaction suspend() throws SystemException {
		final long start = start();
		try {
			return super.suspend();
		}
		finally {
			end(start, "suspend", Level.DEBUG);
		}
	}

	@Override
	public void resume(Transaction tx) throws IllegalStateException, InvalidTransactionException, SystemException {
		final long start = start();
		try {
			super.resume(tx);
		}
		finally {
			end(start, "resume", Level.DEBUG);
		}
	}
}
