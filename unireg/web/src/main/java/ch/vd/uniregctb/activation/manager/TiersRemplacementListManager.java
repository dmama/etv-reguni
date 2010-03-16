package ch.vd.uniregctb.activation.manager;

import ch.vd.uniregctb.activation.view.TiersRemplacementListView;

public interface TiersRemplacementListManager {

	/**
	 * Alimente la vue TiersRemplacementListView
	 *
	 * @param type
	 * @return
	 */
	public TiersRemplacementListView get(String type) ;

}
