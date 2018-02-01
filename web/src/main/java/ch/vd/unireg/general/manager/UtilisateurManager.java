package ch.vd.uniregctb.general.manager;

import ch.vd.uniregctb.general.view.UtilisateurView;

public interface UtilisateurManager {

	/**
	 * Alimente la vue UtilisateurView
	 * @param noIndividuOperateur
	 * @return
	 */
	UtilisateurView get(long noIndividuOperateur);

}
