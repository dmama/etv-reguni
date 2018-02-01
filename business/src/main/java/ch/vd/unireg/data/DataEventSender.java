package ch.vd.unireg.data;

import java.util.List;

import ch.vd.unireg.xml.event.data.v1.DataEvent;

/**
 * Interface d'envoi d'un événement "data" à destination de la partie WS
 * <ul>
 *     <li>envoi des notifications de nettoyage de caches</li>
 *     <li>envoi des demande d'envoi d'événements fiscaux</li>
 * </ul>
 */
public interface DataEventSender {

	/**
	 * Envoi le message vers la partie WS pour les data-events donnés et les événements fiscaux
	 * @param batch liste des événements de notification à envoyer
	 */
	void sendDataEvent(List<DataEvent> batch) throws Exception;
}
