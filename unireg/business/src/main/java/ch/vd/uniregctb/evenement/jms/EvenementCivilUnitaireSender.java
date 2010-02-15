package ch.vd.uniregctb.evenement.jms;

import ch.vd.uniregctb.evenement.EvenementCivilUnitaire;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface EvenementCivilUnitaireSender {
	
	/**
	 * Envoie un événement civil unitaire dans l'ESB
	 *
	 * @param evenement    l'événement à envoyer
	 * @param businessUser le nom métier de l'émetteur
	 * @throws Exception en cas d'erreur dans l'envoi
	 */
	void sendEvent(EvenementCivilUnitaire evenement, String businessUser) throws Exception;
}
