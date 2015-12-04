package ch.vd.uniregctb.admin;

public interface IndexationManager {

	/**
	 * Exécute la réindexation manuelle d'un tiers.
	 * <p/>
	 * Préalablement à l'indexation du tiers, les données du cache du service civil correspondant à l'individu sont évictées.
	 *
	 * @param tiersId le numéro de tiers à réindexer.
	 */
	void reindexTiers(long tiersId);
}
