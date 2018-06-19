package ch.vd.unireg.evenement.organisation;

import java.util.List;

/**
 * Cette classe permet de simuler l'envoi d'événements entreprise
 */
public interface EvenementEntrepriseSender {

	/**
	 * Envoi un événement entreprise dans l'ESB
	 * @param evt l'événement à envoyer
	 * @param businessUser le business user qui fait l'envoi
	 * @param validate Controls validation on send. This allows to disable the validation, useful for testing reaction to invalid messages.
	 * @throws Exception en cas de problème
	 */
	void sendEvent(EvenementEntreprise evt, String businessUser, boolean validate) throws Exception;

	/**
	 * Envoi un événement entreprise ciblant plusieurs entreprises dans l'ESB
	 * @param evt l'événement à envoyer
	 * @param nos les numéro cantonaux ciblés
	 * @param businessUser le business user qui fait l'envoi
	 * @param validate Controls validation on send. This allows to disable the validation, useful for testing reaction to invalid messages.
	 * @throws Exception en cas de problème
	 */
	void sendEventWithMultipleEntreprises(EvenementEntreprise evt, List<Long> nos, String businessUser, boolean validate) throws Exception;
}
