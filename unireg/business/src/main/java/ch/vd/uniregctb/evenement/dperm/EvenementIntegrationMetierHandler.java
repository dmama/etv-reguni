package ch.vd.uniregctb.evenement.dperm;

import java.util.Map;

import org.w3c.dom.Document;

/**
 * Interface implémentée par les handlers spécifiques
 */
public interface EvenementIntegrationMetierHandler {

	/**
	 * Traitement du message
	 * @param xmlInterne xml interne (= encapsulé dans) à l'événement d'intégration métier
	 * @param metaDonnees les méta-données du message entrant
	 * @return le document à renvoyer dans la réponse
	 */
	Document handleMessage(String xmlInterne, Map<String, String> metaDonnees) throws Exception;

}
