package ch.vd.uniregctb.evenement.organisation;

import java.util.List;

/**
 * Cette classe permet de simuler l'envoi d'événements organisation
 */
public interface EvenementOrganisationSender {

	/**
	 * Envoi un événement organisation dans l'ESB
	 * @param evt l'événement à envoyer
	 * @param businessUser le business user qui fait l'envoi
	 * @param validate Controls validation on send. This allows to disable the validation, useful for testing reaction to invalid messages.
	 * @throws Exception en cas de problème
	 */
	void sendEvent(EvenementOrganisation evt, String businessUser, boolean validate) throws Exception;

	/**
	 * Envoi un événement organisation ciblant plusieurs organisations dans l'ESB
	 * @param evt l'événement à envoyer
	 * @param nos les numéro cantonaux ciblés
	 * @param businessUser le business user qui fait l'envoi
	 * @param validate Controls validation on send. This allows to disable the validation, useful for testing reaction to invalid messages.
	 * @throws Exception en cas de problème
	 */
	void sendEventWithMultipleOrga(EvenementOrganisation evt, List<Long> nos, String businessUser, boolean validate) throws Exception;
}
