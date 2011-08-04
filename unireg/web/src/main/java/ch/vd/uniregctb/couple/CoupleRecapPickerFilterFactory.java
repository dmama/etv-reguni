package ch.vd.uniregctb.couple;

import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.picker.TiersPickerFilter;
import ch.vd.uniregctb.tiers.picker.TiersPickerFilterFactory;

public class CoupleRecapPickerFilterFactory implements TiersPickerFilterFactory {

	private TiersDAO tiersDAO;

	@Override
	public TiersPickerFilter parse(String paramsString) {
		return new CoupleRecapPickerFilter(tiersDAO);
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}
}
