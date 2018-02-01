package ch.vd.unireg.transaction;

import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.support.TransactionSynchronization;

/**
 * Interface qui expose uniquement la méthode pour enregistrer une nouvelle synchronisation sur la transaction courante.
 */
@FunctionalInterface
public interface TransactionSynchronizationManagerInterface {

	/**
	 * Register a new transaction synchronization for the current thread. Typically called by resource management code. <p>Note that synchronizations can implement the {@link org.springframework.core.Ordered} interface. They will be executed in an
	 * order according to their order value (if any).
	 *
	 * @param synchronization the synchronization object to register
	 * @throws IllegalStateException if transaction synchronization is not active
	 */
	void registerSynchronization(@NotNull TransactionSynchronization synchronization) throws IllegalStateException;
}
