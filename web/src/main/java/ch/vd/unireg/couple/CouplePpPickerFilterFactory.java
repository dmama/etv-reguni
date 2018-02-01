package ch.vd.unireg.couple;

import ch.vd.unireg.search.SearchTiersFilter;
import ch.vd.unireg.search.SearchTiersFilterFactory;

public class CouplePpPickerFilterFactory implements SearchTiersFilterFactory {

	@Override
	public SearchTiersFilter parse(String paramsString) {
		return new CouplePpPickerFilter();
	}
}
