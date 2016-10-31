package ch.vd.uniregctb.transaction;

import javax.transaction.Synchronization;
import java.util.function.Consumer;

/**
 * Manager spécialisé qui permet d'enregistrer une synchronisation sur la transaction ouverte (par exemple, lors de la réception d'un message ESB).
 * <p/>
 * Ce manager fonctionne comme le {@link org.springframework.transaction.support.TransactionSynchronizationManager} mais sans recourir à une implémentation statique, ce qui permet de le mocker facilement dans les tests unitaires.
 */
@SuppressWarnings("unused")
public interface TxSyncManager {

	/**
	 * Register a {@link Synchronization} callback with this transaction.
	 */
	void registerSynchronization(Synchronization sync);

	/**
	 * Register a {@link Consumer} callback called after completion of the transaction.
	 *
	 * @param consumer a consumer
	 */
	void registerAfterCompletion(Consumer<Integer> consumer);
}
