package ch.vd.uniregctb.tiers.picker;

import java.util.List;

import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;

public interface TiersPickerFilterWithPostFiltering extends TiersPickerFilter {

	/**
	 * Filtre les résultats de la recherche. La liste spécifiée sera modifiée.
	 *
	 * @param list   la liste des résultats de recherche à filtrer
	 */
	void postFilter(List<TiersIndexedData> list);
}
