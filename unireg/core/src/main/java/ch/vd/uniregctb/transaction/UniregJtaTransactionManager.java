package ch.vd.uniregctb.transaction;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Spécificité Unireg du JtaTransactionManager proposé par Spring, afin de faire en sorte qu'un flush soit
 * toujours appelé avant le lancement du commit dans toutes les transactions
 */
public class UniregJtaTransactionManager extends JtaTransactionManager {

	@Override
	protected void prepareSynchronization(DefaultTransactionStatus status, TransactionDefinition definition) {
		super.prepareSynchronization(status, definition);

		// enregistrement d'une synchronisation qui forcera le flush en tout début de phase de commit
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
			@Override
			public int getOrder() {
				// pour que le "beforeCommit" soit appelé en tout début de phase de commit
				return 0;
			}

			@Override
			public void beforeCommit(boolean readOnly) {
				super.beforeCommit(readOnly);
				if (!readOnly && status.isNewTransaction() && !status.isRollbackOnly()) {
					status.flush();
				}
			}
		});
	}
}
