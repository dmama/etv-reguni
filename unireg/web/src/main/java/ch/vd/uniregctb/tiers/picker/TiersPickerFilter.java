package ch.vd.uniregctb.tiers.picker;

import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersFilter;

public interface TiersPickerFilter extends TiersFilter {

	/**
	 * @return une description du filtre qui sera affichée à l'utilisateur
	 */
	String getDescription();

	TiersCriteria.TypeVisualisation getTypeVisualisation();

	TiersCriteria.TypeTiers getTypeTiers();

	boolean isInclureI107();

	boolean isInclureTiersAnnules();

	boolean isTiersAnnulesSeulement();

	Boolean isTiersActif();
}
