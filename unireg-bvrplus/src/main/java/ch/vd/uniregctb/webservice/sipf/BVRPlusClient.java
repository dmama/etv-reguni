package ch.vd.uniregctb.webservice.sipf;

import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrDemande;
import ch.vd.service.sipf.wsdl.sipfbvrplus_v1.BvrReponse;

@SuppressWarnings({"UnusedDeclaration"})
public interface BVRPlusClient {

	/**
	 * Exécute la demande de numéro BVR spécifiée.
	 *
	 * @param bvrDemande une demande de numéro BVR.
	 * @return la réponse du service
	 * @throws BVRPlusClientException en cas d'erreur de communication ou d'erreur levée par le service BVR.
	 */
	public BvrReponse getBVRDemande(BvrDemande bvrDemande) throws BVRPlusClientException;

	/**
	 * Envoie une requête de ping au service pour s'assurer que la connexion est bien établie.
	 *
	 * @throws BVRPlusClientException si le service n'est pas connecté.
	 */
	public void ping() throws BVRPlusClientException;
}
