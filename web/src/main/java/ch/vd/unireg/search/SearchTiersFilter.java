package ch.vd.unireg.search;

import java.util.Set;

import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.TiersFilter;

public interface SearchTiersFilter extends TiersFilter {

	/**
	 * @return une description du filtre qui sera affichée à l'utilisateur
	 */
	String getDescription();

	@Override
	TiersCriteria.TypeVisualisation getTypeVisualisation();

	@Override
	Set<TiersCriteria.TypeTiers> getTypesTiers();

	@Override
	boolean isInclureI107();

	@Override
	boolean isInclureTiersAnnules();

	@Override
	boolean isTiersAnnulesSeulement();

	@Override
	Boolean isTiersActif();
}
