package ch.vd.uniregctb.couple;

import ch.vd.uniregctb.search.SearchTiersFilter;
import ch.vd.uniregctb.search.SearchTiersFilterFactory;

public class CouplePpPickerFilterFactory implements SearchTiersFilterFactory {

	@Override
	public SearchTiersFilter parse(String paramsString) {
		return new CouplePpPickerFilter();
	}
}
