package ch.vd.unireg.general.manager;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.general.view.UtilisateurView;

public interface UtilisateurManager {

	/**
	 * Alimente la vue UtilisateurView
	 */
	UtilisateurView get(@NotNull String visaOperateur);

}
