package ch.vd.uniregctb.transaction;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import java.util.function.Consumer;

/**
 * Un pseudo-synchronisation manager qui appelle immédiatement le callback sans se soucier de la transaction.
 */
public class MockTxSyncManager implements TxSyncManager {
	@Override
	public void registerSynchronization(Synchronization sync) {
		sync.afterCompletion(Status.STATUS_COMMITTED);
	}

	@Override
	public void registerAfterCompletion(Consumer<Integer> consumer) {
		consumer.accept(Status.STATUS_COMMITTED);
	}
}
