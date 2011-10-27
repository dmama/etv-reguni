package ch.vd.uniregctb.tiers.picker;

import java.util.List;

import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;

public class TiersPickerManagerImpl implements TiersPickerManager {
	@Override
	public void postFilter(TiersPickerFilterWithPostFiltering filter, List<TiersIndexedData> list) {
		filter.postFilter(list);
	}
}
