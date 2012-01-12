package ch.vd.uniregctb.couple;

import ch.vd.uniregctb.tiers.picker.TiersPickerFilter;
import ch.vd.uniregctb.tiers.picker.TiersPickerFilterFactory;

public class CouplePpPickerFilterFactory implements TiersPickerFilterFactory {

	@Override
	public TiersPickerFilter parse(String paramsString) {
		return new CouplePpPickerFilter();
	}
}
