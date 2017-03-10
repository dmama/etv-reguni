package ch.vd.uniregctb.evenement.degrevement;

import java.util.Map;

import ch.vd.unireg.xml.event.degrevement.v1.Message;
import ch.vd.uniregctb.jms.EsbBusinessException;

/**
 * Interface du handler métier (hors de toute considération de parsing de message XML...)
 * du traitement du retour des données de dégrèvement
 */
public interface EvenementDegrevementHandlerV1 {

	/**
	 * Traitement des données de dégrèvement
	 * @param retour données reçues (scannage, démat...)
	 * @param headers méta-information reçues sur le message entrant
	 * @throws EsbBusinessException en cas de souci métier
	 */
	void onRetourDegrevement(Message retour, Map<String, String> headers) throws EsbBusinessException;

}
