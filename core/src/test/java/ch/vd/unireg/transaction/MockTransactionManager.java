package ch.vd.unireg.transaction;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.CallbackPreferringPlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.utils.NotImplementedException;

/**
 * Mock du transaction manager qui ne gère aucune transaction et délègue les opérations aux callbacks.
 */
public class MockTransactionManager implements CallbackPreferringPlatformTransactionManager {
	@Override
	public <T> T execute(TransactionDefinition definition, TransactionCallback<T> callback) throws TransactionException {
		return callback.doInTransaction(getTransaction(definition));
	}

	@Override
	public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
		return new SimpleTransactionStatus(false);
	}

	@Override
	public void commit(TransactionStatus status) throws TransactionException {
		// rien à faire
	}

	@Override
	public void rollback(TransactionStatus status) throws TransactionException {
		throw new NotImplementedException("Il n'est pas sensé y avoir de rollback avec le MockTransactionManager");
	}
}
