package ch.vd.uniregctb.tiers.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.lr.view.ListeRecapDetailView;

/**
 * Service qui fournit les methodes pour visualiser une liste recapitulative
 *
 * @author xcifde
 *
 */
public interface ListeRecapVisuManager {

	/**
	 * Charge les informations dans ListeRecapitulativeView
	 *
	 * @param numero
	 * @return un objet ListeRecapDetailView
	 */
	@Transactional(readOnly = true)
	public ListeRecapDetailView get(Long numero) ;

}
