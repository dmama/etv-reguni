package ch.vd.uniregctb.couple;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.search.SearchTiersFilterWithPostFiltering;
import ch.vd.uniregctb.tiers.TiersCriteria;

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
		final Set<TiersCriteria.TypeTiers> set = new HashSet<TiersCriteria.TypeTiers>();
		set.add(TiersCriteria.TypeTiers.PERSONNE_PHYSIQUE);
		return set;
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
