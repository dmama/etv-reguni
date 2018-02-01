package ch.vd.unireg.fusion.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.fusion.view.FusionListView;

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
	FusionListView get(Long numeroNonHab) ;

}
