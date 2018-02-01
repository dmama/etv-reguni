package ch.vd.unireg.indexer.tiers;

import ch.vd.unireg.common.StatusManager;

public interface OfficeImpotIndexer {

	/**
	 * Calcul et stocke l'office d'impôt sur tous les tiers dont l'office d'impôt est inconnu.
	 */
	void indexTiersAvecOfficeImpotInconnu(StatusManager status);

	/**
	 * Recalcul et stocke l'office d'impôt sur tous les tiers sans exception.
	 */
	void indexTousLesTiers(StatusManager status);
}
