package ch.vd.capitastra.common;

import ch.vd.capitastra.grundstueck.GrundstueckFlaeche;

/**
 * Interface pour les immeubles qui possèdent une surface totale (qui peut être nulle en elle-même).
 */
public interface GrundstueckMitFlaeche {
	/**
	 * @return la surface totale de l'immeubles.
	 */
	GrundstueckFlaeche getGrundstueckFlaeche();
}
