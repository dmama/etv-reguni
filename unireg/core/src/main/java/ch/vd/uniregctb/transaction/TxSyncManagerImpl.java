package ch.vd.uniregctb.transaction;

import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("unused")
public class TxSyncManagerImpl implements TxSyncManager {

	@Autowired
	private TransactionManager transactionManager;

	public void setTransactionManager(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	public void registerSynchronization(Synchronization sync) {
		final Transaction tx;
		try {
			tx = transactionManager.getTransaction();
			if (tx == null) {
				throw new IllegalArgumentException("Il n'y a pas de transaction liée au thread courant.");
			}
			tx.registerSynchronization(sync);
		}
		catch (SystemException | RollbackException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void registerAfterCompletion(Consumer<Integer> consumer) {
		registerSynchronization(new Synchronization() {
			@Override
			public void beforeCompletion() {
			}

			@Override
			public void afterCompletion(int status) {
				consumer.accept(status);
			}
		});
	}
}
