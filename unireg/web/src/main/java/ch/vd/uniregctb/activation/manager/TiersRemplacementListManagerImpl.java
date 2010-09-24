package ch.vd.uniregctb.activation.manager;

import ch.vd.uniregctb.activation.view.TiersRemplacementListView;

public class TiersRemplacementListManagerImpl implements TiersRemplacementListManager{

	/**
	 * Alimente la vue TiersRemplacementListView
	 *
	 * @param type
	 * @return
	 */
	public TiersRemplacementListView get(String type) {
		TiersRemplacementListView tiersRemplacementListView = new TiersRemplacementListView();
		tiersRemplacementListView.setType(type);
		return tiersRemplacementListView;
	}

}
