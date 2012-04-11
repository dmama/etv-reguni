package ch.vd.uniregctb.couple;

import ch.vd.uniregctb.search.SearchTiersFilter;
import ch.vd.uniregctb.search.SearchTiersFilterFactory;
import ch.vd.uniregctb.tiers.TiersDAO;

public class CoupleMcPickerFilterFactory implements SearchTiersFilterFactory {

	private TiersDAO tiersDAO;

	@Override
	public SearchTiersFilter parse(String paramsString) {
		return new CoupleMcPickerFilter(tiersDAO);
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}
}
