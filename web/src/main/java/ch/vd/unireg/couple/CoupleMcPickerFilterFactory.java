package ch.vd.unireg.couple;

import ch.vd.unireg.search.SearchTiersFilter;
import ch.vd.unireg.search.SearchTiersFilterFactory;
import ch.vd.unireg.tiers.TiersDAO;

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
