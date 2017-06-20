package ch.vd.uniregctb.transaction;

import java.util.function.Consumer;

import org.springframework.transaction.support.TransactionSynchronization;

/**
 * Interface implémentée par les entités qui veulent être notifiées de chaque début de transaction
 * pour y enregistrer leurs synchronizations
 */
public interface TransactionSynchronizationSupplier {

	/**
	 * Méthode appelée en début de transaction afin que le composant puisse enregistrer ses synchronisations
	 * @param collector collecteur des synchronisations à enregistrer
	 */
	void registerSynchronizations(Consumer<TransactionSynchronization> collector);
}
