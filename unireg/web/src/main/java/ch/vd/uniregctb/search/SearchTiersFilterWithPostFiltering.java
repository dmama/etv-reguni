package ch.vd.uniregctb.search;

import java.util.List;

import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;

public interface SearchTiersFilterWithPostFiltering extends SearchTiersFilter {

	/**
	 * Filtre les résultats de la recherche. La liste spécifiée sera modifiée.
	 *
	 * @param list la liste des résultats de recherche à filtrer
	 */
	void postFilter(List<TiersIndexedData> list);
}
