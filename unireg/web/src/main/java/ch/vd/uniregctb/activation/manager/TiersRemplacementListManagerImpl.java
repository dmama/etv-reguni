package ch.vd.uniregctb.activation.manager;

import ch.vd.uniregctb.activation.view.TiersRemplacementListView;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TypeTiers;

public class TiersRemplacementListManagerImpl implements TiersRemplacementListManager{

	/**
	 * Alimente la vue TiersRemplacementListView
	 *
	 * @param type
	 * @return
	 */
	public TiersRemplacementListView get(TypeTiers type) {
		TiersRemplacementListView tiersRemplacementListView = new TiersRemplacementListView();
		tiersRemplacementListView.setTypeTiers(TiersCriteria.TypeTiers.fromCore(type));
		return tiersRemplacementListView;
	}
}
