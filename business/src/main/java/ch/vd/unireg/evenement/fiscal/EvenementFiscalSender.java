package ch.vd.uniregctb.evenement.fiscal;

/**
 * Interface qui permet d'envoyer des événements fiscaux
 */
public interface EvenementFiscalSender {

	String VERSION_ATTRIBUTE = "evenementVersion";

	/**
	 * Envoie l'événement comme message JMS.
	 *
	 * @param evenement un événement fiscal
	 * @throws EvenementFiscalException si un problème survient durant la génération du XML ou durant la transmission du message au serveur JMS.
	 */
	void sendEvent(EvenementFiscal evenement) throws EvenementFiscalException;
}
