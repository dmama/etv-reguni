package ch.vd.uniregctb.transaction;

import java.util.function.Consumer;

import org.springframework.transaction.support.TransactionSynchronization;

/**
 * Un pseudo-synchronisation manager qui appelle immédiatement le callback sans se soucier de la transaction.
 */
public class MockTxSyncManager implements TxSyncManager {
	@Override
	public void registerAfterCompletion(Consumer<Integer> consumer) {
		consumer.accept(TransactionSynchronization.STATUS_COMMITTED);
	}
}
