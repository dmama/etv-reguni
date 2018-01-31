package ch.vd.unireg.interfaces.organisation;

/**
 * Interface implémentée par un service organisation qui en wrap un autre (proxy, cache...)
 */
public interface ServiceOrganisationServiceWrapper {

	/**
	 * @return le service immédiatement wrappé
	 */
	ServiceOrganisationRaw getTarget();


	/**
	 * @return le service wrappé en bout de chaîne
	 */
	ServiceOrganisationRaw getUltimateTarget();
}
