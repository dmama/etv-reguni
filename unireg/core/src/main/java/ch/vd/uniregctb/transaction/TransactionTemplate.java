package ch.vd.uniregctb.transaction;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

/**
 * Transaction template qui force un flush de la session pendant la transaction encore et pas
 * seulement comme un effet de bord du commit.<p/>
 * En effet, cela semble poser des soucis à Hibernate4 qui voit une transaction "markedForRollback" et ne s'en sort plus...
 */
public class TransactionTemplate extends org.springframework.transaction.support.TransactionTemplate {

	public TransactionTemplate() {
	}

	public TransactionTemplate(PlatformTransactionManager transactionManager) {
		super(transactionManager);
	}

	public TransactionTemplate(PlatformTransactionManager transactionManager, TransactionDefinition transactionDefinition) {
		super(transactionManager, transactionDefinition);
	}

	@Override
	public <T> T execute(final TransactionCallback<T> action) throws TransactionException {
		return super.execute(new TransactionCallback<T>() {
			@Override
			public T doInTransaction(TransactionStatus status) {
				final T result = action.doInTransaction(status);
				if (status.isNewTransaction() && !status.isRollbackOnly() && !isReadOnly()) {
					status.flush();
				}
				return result;
			}
		});
	}
}
