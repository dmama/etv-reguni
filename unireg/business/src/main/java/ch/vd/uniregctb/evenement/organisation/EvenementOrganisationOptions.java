package ch.vd.uniregctb.evenement.organisation;

/**
 * Différentes options de comportement disponibles dans le traitement des événements organisation.
 */
public class EvenementOrganisationOptions {

	private final boolean refreshCache;

	public String traitementManuelMessage;

	public EvenementOrganisationOptions(boolean refreshCache) {
		this.refreshCache = refreshCache;
	}

	public boolean isRefreshCache() {
		return refreshCache;
	}

	public String getTraitementManuelMessage() {
		return traitementManuelMessage;
	}

	public void setTraitementManuelMessage(String traitementManuelMessage) {
		this.traitementManuelMessage = traitementManuelMessage;
	}
}
