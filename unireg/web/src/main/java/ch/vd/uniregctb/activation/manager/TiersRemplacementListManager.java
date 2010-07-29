package ch.vd.uniregctb.activation.manager;

import org.springframework.transaction.annotation.Transactional;

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
