package ch.vd.uniregctb.evenement.civil.common;

/**
 * Différentes options de comportement disponibles dans le traitement des événements civils.
 */
public class EvenementCivilOptions {

	private final boolean refreshCache;

	public EvenementCivilOptions(boolean refreshCache) {
		this.refreshCache = refreshCache;
	}

	public boolean isRefreshCache() {
		return refreshCache;
	}
}
