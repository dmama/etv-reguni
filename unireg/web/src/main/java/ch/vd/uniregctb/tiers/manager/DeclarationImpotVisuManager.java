package ch.vd.uniregctb.tiers.manager;

import ch.vd.uniregctb.di.view.DeclarationImpotDetailView;

/**
 * Service qui fournit les methodes pour visualiser une declaration d'impot
 *
 * @author xcifde
 *
 */
public interface DeclarationImpotVisuManager {

	/**
	 * Charge les informations dans DeclarationImpotOrdinaireView
	 *
	 * @param numero
	 * @return un objet DeclarationImpotOrdinaireView
	 */
	public DeclarationImpotDetailView get(Long numero) ;

}
