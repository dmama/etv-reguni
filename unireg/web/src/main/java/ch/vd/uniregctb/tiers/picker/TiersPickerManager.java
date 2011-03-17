package ch.vd.uniregctb.tiers.picker;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;

public interface TiersPickerManager {

	/**
	 * Execute la méthode de post-filtrage dans un environnement transactionel
	 *
	 * @param filter le filtre à appliquer
	 * @param list   la liste des résultats de recherche à filtrer
	 */
	@Transactional(readOnly = true)
	void postFilter(TiersPickerFilterWithPostFiltering filter, List<TiersIndexedData> list);
}
