package ch.vd.uniregctb.tiers;

import java.util.Set;

/**
 * Interface qui expose les filtres à appliquer sur les résultats de recherche de tiers.
 */
public interface TiersFilter {

	TiersCriteria.TypeVisualisation getTypeVisualisation();

	Set<TiersCriteria.TypeTiers> getTypesTiers();

	boolean isInclureI107();

	boolean isInclureTiersAnnules();

	boolean isTiersAnnulesSeulement();

	Boolean isTiersActif();
}
