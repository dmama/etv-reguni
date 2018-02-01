package ch.vd.uniregctb.jms;

import ch.vd.technical.esb.EsbMessage;

/**
 * Interface des réelles implémentations de réception d'événements JMS/ESB
 */
public interface EsbMessageHandler {

	/**
	 * Appelé avec le message entrant, dans le cadre d'une transaction
	 * @param message le message entrant
	 * @throws EsbBusinessException en cas de problème métier à envoyer en queue d'erreur
	 * @throws Exception en cas de souci... causera un renvoi en DLQ
	 */
	void onEsbMessage(EsbMessage message) throws Exception;
}
