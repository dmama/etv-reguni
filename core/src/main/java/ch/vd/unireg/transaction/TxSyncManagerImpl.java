package ch.vd.unireg.transaction;

import java.util.function.Consumer;

import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SuppressWarnings("unused")
public class TxSyncManagerImpl implements TxSyncManager {

	@Override
	public void registerAfterCompletion(Consumer<Integer> consumer) {
		if (!TransactionSynchronizationManager.isActualTransactionActive()) {
			throw new IllegalStateException("Il n'y a pas de transaction liée au thread courant.");
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
			@Override
			public void afterCompletion(int status) {
				super.afterCompletion(status);
				consumer.accept(status);
			}
		});
	}
}
