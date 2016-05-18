package ch.vd.uniregctb.evenement.organisation;

import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationMessageCollector;

/**
 * Exception lancée par le processing des événements organisation quand on aimerait
 * quand-même conserver les messages collectés jusque là (le cas du capping est un exemple)
 */
public class EvenementOrganisationConservationMessagesException extends EvenementOrganisationException {

	private final EvenementOrganisationMessageCollector<EvenementOrganisationErreur> messageCollector;
	private final boolean keepStack;

	public EvenementOrganisationConservationMessagesException(String message, EvenementOrganisationMessageCollector<EvenementOrganisationErreur> messageCollector, boolean keepStack) {
		super(message);
		this.messageCollector = messageCollector;
		this.keepStack = keepStack;
	}

	public EvenementOrganisationMessageCollector<EvenementOrganisationErreur> getMessageCollector() {
		return messageCollector;
	}

	public boolean keepStack() {
		return keepStack;
	}
}
