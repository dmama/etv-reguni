package ch.vd.uniregctb.couple.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.couple.view.CoupleListView;


/**
 * Service offrant des methodes pour le controller PersonnePhysiqueListController
 *
 * @author xcifde
 *
 */
public interface CoupleListManager {

	/**
	 * Alimente la vue CoupleListView (cas ou numeroPP selectionne)
	 *
	 * @param numeroPP
	 * @return une vue CoupleListView
	 */
	@Transactional(readOnly = true)
	public CoupleListView get(Long numeroPP) ;

	/**
	 * Alimente la vue CoupleListView (cas ou numeroPP non selectionne)
	 *
	 * @return une vue CoupleListView
	 */
	public CoupleListView get() ;

}
