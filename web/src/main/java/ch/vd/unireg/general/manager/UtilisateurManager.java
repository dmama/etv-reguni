package ch.vd.unireg.general.manager;

import ch.vd.unireg.general.view.UtilisateurView;

public interface UtilisateurManager {

	/**
	 * Alimente la vue UtilisateurView
	 * @param noIndividuOperateur
	 * @return
	 */
	UtilisateurView get(long noIndividuOperateur);

}
