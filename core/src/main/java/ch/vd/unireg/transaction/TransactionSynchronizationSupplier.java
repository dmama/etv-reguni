package ch.vd.uniregctb.transaction;

import org.jetbrains.annotations.NotNull;

/**
 * Interface implémentée par les entités qui veulent être notifiées de chaque début de transaction pour y enregistrer leurs synchronizations
 */
public interface TransactionSynchronizationSupplier {

	/**
	 * Méthode appelée en début de transaction afin que le composant puisse enregistrer ses synchronisations (<b>attention</b>, cette méthode peut-être appelée plusieurs fois par transaction, les implémentations doivent donc s'assurer au mieux de ne
	 * pas ré-enregistrer les mêmes synchronisations plusieurs fois...)
	 *
	 * @param mgr le manager pour enregistrer la synchronisation
	 */
	void registerSynchronizations(@NotNull TransactionSynchronizationManagerInterface mgr);
}
