package ch.vd.uniregctb.data;

import java.util.List;

/**
 * Interface d'interaction directe avec le DataEventSender
 */
public interface DataEventSender {

	/**
	 * Envoi par le canal "data" d'une demande d'envoi des événements fiscaux dont les identifiants sont fournis
	 * @param idsEvenementsFiscaux identifiants des événements fiscaux à envoyer
	 */
	void sendEvenementsFiscaux(List<Long> idsEvenementsFiscaux);
}
