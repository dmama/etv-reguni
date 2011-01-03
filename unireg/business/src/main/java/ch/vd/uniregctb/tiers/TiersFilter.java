package ch.vd.uniregctb.tiers;

/**
 * Interface qui expose les filtres à appliquer sur les résultats de recherche de tiers.
 */
public interface TiersFilter {

	TiersCriteria.TypeVisualisation getTypeVisualisation();

	TiersCriteria.TypeTiers getTypeTiers();

	boolean isInclureI107();

	boolean isInclureTiersAnnules();

	boolean isTiersAnnulesSeulement();

	Boolean isTiersActif();
}
