package ch.vd.unireg.interfaces.entreprise;

/**
 * Interface implémentée par un service entreprise qui en wrap un autre (proxy, cache...)
 */
public interface ServiceEntrepriseWrapper {

	/**
	 * @return le service immédiatement wrappé
	 */
	ServiceEntrepriseRaw getTarget();


	/**
	 * @return le service wrappé en bout de chaîne
	 */
	ServiceEntrepriseRaw getUltimateTarget();
}
