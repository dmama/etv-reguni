package ch.vd.unireg.tiers.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.lr.view.ListeRecapitulativeDetailView;

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
	ListeRecapitulativeDetailView get(Long numero) ;

}
