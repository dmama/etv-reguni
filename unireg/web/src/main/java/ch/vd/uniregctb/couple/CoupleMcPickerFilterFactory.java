package ch.vd.uniregctb.couple;

import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.picker.TiersPickerFilter;
import ch.vd.uniregctb.tiers.picker.TiersPickerFilterFactory;

public class CoupleMcPickerFilterFactory implements TiersPickerFilterFactory {

	private TiersDAO tiersDAO;

	@Override
	public TiersPickerFilter parse(String paramsString) {
		return new CoupleMcPickerFilter(tiersDAO);
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}
}
