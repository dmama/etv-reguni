package ch.vd.uniregctb.fusion.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.fusion.view.FusionListView;

/**
 * Service mettant a disposition des methodes pour le controller FusionListController
 *
 * @author xcifde
 *
 */
public interface FusionListManager {

	/**
	 * Alimente la vue FusionListView
	 *
	 * @param numeroNonHab
	 * @return une vue FusionListView
	 */
	@Transactional(readOnly = true)
	public FusionListView get(Long numeroNonHab) ;

}
