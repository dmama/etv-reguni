package ch.vd.unireg.couple;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.search.SearchTiersFilterWithPostFiltering;
import ch.vd.unireg.tiers.TiersCriteria;

/**
 * Filtre spécialisé pour l'écran de recherche de la première ou la seconde personne physique dans la constitution d'un couple.
 */
public class CouplePpPickerFilter implements SearchTiersFilterWithPostFiltering {

	public CouplePpPickerFilter() {
	}

	@Override
	public String getDescription() {
		return "recherche limitée aux personnes physiques";
	}

	@Override
	public TiersCriteria.TypeVisualisation getTypeVisualisation() {
		return TiersCriteria.TypeVisualisation.COMPLETE;
	}

	@Override
	public Set<TiersCriteria.TypeTiers> getTypesTiers() {
		return EnumSet.of(TiersCriteria.TypeTiers.PERSONNE_PHYSIQUE);
	}

	@Override
	public boolean isInclureI107() {
		return false;
	}

	@Override
	public boolean isInclureTiersAnnules() {
		return false;
	}

	@Override
	public boolean isTiersAnnulesSeulement() {
		return false;
	}

	@Override
	public Boolean isTiersActif() {
		return null;
	}

	@Override
	public void postFilter(List<TiersIndexedData> list) {
	}

}
