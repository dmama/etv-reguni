package ch.vd.uniregctb.evenement.organisation;

import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationMessageCollector;

/**
 * Exception lancée par le processing des événements organisation quand on aimerait
 * quand-même conserver les messages collectés jusque là (le cas du capping est un exemple)
 */
public class EvenementOrganisationConservationMessagesException extends EvenementOrganisationException {

	private final EvenementOrganisationMessageCollector<EvenementOrganisationErreur> messageCollector;

	public EvenementOrganisationConservationMessagesException(String message, EvenementOrganisationMessageCollector<EvenementOrganisationErreur> messageCollector) {
		super(message);
		this.messageCollector = messageCollector;
	}

	public EvenementOrganisationMessageCollector<EvenementOrganisationErreur> getMessageCollector() {
		return messageCollector;
	}
}
