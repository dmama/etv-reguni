package ch.vd.uniregctb.transaction;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import ch.vd.uniregctb.common.IdentityKey;
import ch.vd.uniregctb.common.LockHelper;

/**
 * Spécificité Unireg du JtaTransactionManager proposé par Spring, afin de faire en sorte qu'un flush soit
 * toujours appelé avant le lancement du commit dans toutes les transactions
 */
public class UniregJtaTransactionManager extends JtaTransactionManager implements TransactionSynchronizationRegistrar {

	/**
	 * Fournisseurs de synchronisations enregistrés
	 */
	private final Set<IdentityKey<TransactionSynchronizationSupplier>> synchronizationSuppliers = new HashSet<>();

	/**
	 * Verrou d'accès à la collection des fournisseurs de synchronisations
	 */
	private final LockHelper lockHelper = new LockHelper();

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

		// interrogation des composants qui se sont enregistrés pour fournir leurs synchronisations
		registerSuppliedSynchronizations();
	}

	@Override
	public void registerSynchronizationSupplier(TransactionSynchronizationSupplier supplier) {
		Objects.requireNonNull(supplier, "Supplier is not expected to be null");
		lockHelper.doInWriteLock(() -> synchronizationSuppliers.add(new IdentityKey<>(supplier)));
	}

	@Override
	public void unregisterSynchronizationSupplier(TransactionSynchronizationSupplier supplier) {
		Objects.requireNonNull(supplier, "Supplier is not expected to be null");
		lockHelper.doInWriteLock(() -> synchronizationSuppliers.remove(new IdentityKey<>(supplier)));
	}

	private void registerSuppliedSynchronizations() {
		final Consumer<TransactionSynchronization> collector = TransactionSynchronizationManager::registerSynchronization;
		lockHelper.doInReadLock(() -> synchronizationSuppliers.stream()
				.map(IdentityKey::getElt)
				.forEach(supplier -> supplier.registerSynchronizations(collector)));
	}
}
