package ch.vd.unireg.transaction;

import java.util.function.Consumer;

import org.springframework.transaction.support.TransactionSynchronization;

/**
 * Manager spécialisé qui permet d'enregistrer une synchronisation sur la transaction ouverte (par exemple, lors de la réception d'un message ESB).
 * <p/>
 * Ce manager fonctionne comme le {@link org.springframework.transaction.support.TransactionSynchronizationManager} mais sans recourir à une implémentation statique, ce qui permet de le mocker facilement dans les tests unitaires.
 */
@SuppressWarnings("unused")
public interface TxSyncManager {

	/**
	 * Register a {@link Consumer} callback called after completion of the transaction.
	 *
	 * @param consumer a consumer
	 * @see TransactionSynchronization#STATUS_COMMITTED
	 */
	void registerAfterCompletion(Consumer<Integer> consumer);
}
