package ch.vd.uniregctb.activation.manager;

import ch.vd.uniregctb.activation.view.TiersActivationListView;

public interface TiersActivationListManager {

	/**
	 * Alimente la vue TiersActivationListView
	 *
	 * @param activation
	 * @return
	 */
	public TiersActivationListView get(String activation) ;

}
