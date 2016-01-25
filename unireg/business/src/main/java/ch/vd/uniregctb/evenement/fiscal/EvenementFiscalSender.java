package ch.vd.uniregctb.evenement.fiscal;

import ch.vd.uniregctb.evenement.EvenementFiscal;


/**
 * Interface qui permet d'envoyer des événements externes.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface EvenementFiscalSender {

	/**
	 * Envoie l'événement comme message JMS.
	 *
	 * @param evenement un événement fiscal
	 * @throws EvenementFiscalException si un problème survient durant la génération du XML ou durant la transmission du message au serveur JMS.
	 */
	void sendEvent(EvenementFiscal evenement) throws EvenementFiscalException;
}
