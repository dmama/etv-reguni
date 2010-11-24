package ch.vd.uniregctb.activation.manager;

import ch.vd.uniregctb.activation.view.TiersRemplacementListView;
import ch.vd.uniregctb.tiers.TypeTiers;

public interface TiersRemplacementListManager {

	/**
	 * Alimente la vue TiersRemplacementListView
	 *
	 * @param type
	 * @return
	 */
	public TiersRemplacementListView get(TypeTiers type) ;
}
