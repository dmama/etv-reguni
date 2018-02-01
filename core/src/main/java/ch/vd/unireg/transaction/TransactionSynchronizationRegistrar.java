package ch.vd.uniregctb.transaction;

/**
 * Interface implémentée par l'entité dans laquelle les composants qui ont des synchronisations à enregistrer
 * doivent eux-même s'enregistrer
 */
public interface TransactionSynchronizationRegistrar {

	/**
	 * Enregistrement d'un composant prêt à fournir des synchronisations
	 * @param supplier le fournisseur de synchronisations
	 */
	void registerSynchronizationSupplier(TransactionSynchronizationSupplier supplier);

	/**
	 * Dés-inscription d'un composant prêt à fournir des synchronisations (égalité basée sur l'identité)
	 * @param supplier le fournisseur de synchronisations
	 */
	void unregisterSynchronizationSupplier(TransactionSynchronizationSupplier supplier);

}
