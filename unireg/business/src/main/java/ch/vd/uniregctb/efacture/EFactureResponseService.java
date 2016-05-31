package ch.vd.uniregctb.efacture;

/**
 * Interface du service de gestion des réponses (= comportement pseudo-synchrone) de l'application e-facture
 */
public interface EFactureResponseService {

	/**
	 * Appelé à la réception d'une nouvelle réponse, identifiée par le businessId du message original
	 * @param businessId businessId du message original
	 */
	void onNewResponse(String businessId);

	/**
	 * Attend la réponse correspondant au businessId donné, mais pas plus longtemps que le timeout
	 * @param businessId businessId du message original
	 * @param timeoutMs timeout en millisecondes maximal (strictement positif !)
	 * @return <code>true</code> si la réponse est revenue dans les temps, <code>false</code> si le temps n'a pas suffit
	 */
	boolean waitForResponse(String businessId, long timeoutMs);
}
