package ch.vd.unireg.transaction;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import ch.vd.unireg.common.IdentityKey;
import ch.vd.unireg.common.LockHelper;

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

	public UniregJtaTransactionManager(UserTransaction userTransaction, TransactionManager transactionManager) {
		super(userTransaction, transactionManager);
		this.logger = LogFactory.getLog(JtaTransactionManager.class);   // pour éviter de se faire spammer par la classe de base Spring
	}

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
		lockHelper.doInReadLock(() -> synchronizationSuppliers.stream()
				.map(IdentityKey::getElt)
				.forEach(supplier -> supplier.registerSynchronizations(TransactionSynchronizationManager::registerSynchronization)));
	}
}
