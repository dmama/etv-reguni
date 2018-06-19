package ch.vd.unireg.evenement.organisation;

import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseMessageCollector;

/**
 * Exception lancée par le processing des événements entreprise quand on aimerait
 * quand-même conserver les messages collectés jusque là (le cas du capping est un exemple)
 */
public class EvenementEntrepriseConservationMessagesException extends EvenementEntrepriseException {

	private final EvenementEntrepriseMessageCollector<EvenementEntrepriseErreur> messageCollector;
	private final boolean keepStack;

	public EvenementEntrepriseConservationMessagesException(String message, EvenementEntrepriseMessageCollector<EvenementEntrepriseErreur> messageCollector, boolean keepStack) {
		super(message);
		this.messageCollector = messageCollector;
		this.keepStack = keepStack;
	}

	public EvenementEntrepriseMessageCollector<EvenementEntrepriseErreur> getMessageCollector() {
		return messageCollector;
	}

	public boolean keepStack() {
		return keepStack;
	}
}
