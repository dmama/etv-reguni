package ch.vd.uniregctb.di.view;

import ch.vd.uniregctb.general.view.TiersGeneralView;

/**
 * Interface de base des views des déclarations d'impôt
 */
public interface DeclarationImpotView {

	/**
	 * @return le contribuable lié aux déclarations d'impôt qui nous intéressent
	 */
	TiersGeneralView getContribuable();
}
