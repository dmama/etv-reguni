package ch.vd.uniregctb.evenement.jms;

import ch.vd.uniregctb.evenement.EvenementCivilData;

/**
 * Cette classe permet de similuer l'envoi d'événements civils. Utilisé uniquement pour les tests, donc.
 * 
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface EvenementCivilSender {
	
	/**
	 * Envoie un événement civil unitaire dans l'ESB
	 *
	 * @param evenement    l'événement à envoyer
	 * @param businessUser le nom métier de l'émetteur
	 * @throws Exception en cas d'erreur dans l'envoi
	 */
	void sendEvent(EvenementCivilData evenement, String businessUser) throws Exception;
}
