package ch.vd.unireg.evenement.degrevement;

import java.util.Map;

import ch.vd.unireg.xml.degrevement.quittance.v1.QuittanceIntegrationMetierImmDetails;
import ch.vd.unireg.xml.event.degrevement.v1.Message;
import ch.vd.unireg.jms.EsbBusinessException;

/**
 * Interface du handler métier (hors de toute considération de parsing de message XML...)
 * du traitement du retour des données de dégrèvement
 */
public interface EvenementDegrevementHandler {

	/**
	 * Traitement des données de dégrèvement
	 * @param retour données reçues (scannage, démat...)
	 * @param headers méta-information reçues sur le message entrant
	 * @throws EsbBusinessException en cas de souci métier
	 * @return le contenu du message à renvoyer en réponse
	 */
	QuittanceIntegrationMetierImmDetails onRetourDegrevement(Message retour, Map<String, String> headers) throws EsbBusinessException;

}
