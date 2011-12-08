package ch.vd.uniregctb.tiers.picker;

import java.util.Set;

import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersFilter;

public interface TiersPickerFilter extends TiersFilter {

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
