package ch.vd.uniregctb.evenement.docsortant;

import ch.vd.unireg.xml.event.docsortant.v1.Documents;

/**
 * Service d'envoi dans l'ESB des messages déclarant l'envoi d'un document sortant
 */
public interface EvenementDocumentSortantSender {

	/**
	 * Envoi un événement vers l'ESB
	 *
	 * @param businessId businessId du message ESB
	 * @param docs les documents à envoyer
	 * @throws EvenementDocumentSortantException en cas de souci
	 */
	void sendEvenementDocumentSortant(String businessId, Documents docs) throws EvenementDocumentSortantException;
}
