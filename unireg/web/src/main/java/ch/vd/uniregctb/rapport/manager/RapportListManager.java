package ch.vd.uniregctb.rapport.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.rapport.view.RapportListView;

/**
 * Classe offrant les services à RapportListController
 *
 * @author xcifde
 *
 */
public interface RapportListManager {

	/**
	 * Alimente la vue RapportListView (cas ou numero selectionne)
	 *
	 * @param numero
	 * @return une vue RapportListView
	 */
	@Transactional(readOnly = true)
	public RapportListView get(Long numero) ;

}
