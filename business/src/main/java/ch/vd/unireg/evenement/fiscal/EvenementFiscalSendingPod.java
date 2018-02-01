package ch.vd.uniregctb.evenement.fiscal;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plaque tournante de l'envoi des événements fiscaux (multi-version) qui lance tous les "senders" spécifiques
 */
public class EvenementFiscalSendingPod implements EvenementFiscalSender {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementFiscalSendingPod.class);

	private List<EvenementFiscalSender> senders;
	private boolean enabled;

	public void setSenders(List<EvenementFiscalSender> senders) {
		this.senders = senders;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public void sendEvent(EvenementFiscal evenement) throws EvenementFiscalException {

		if (evenement == null) {
			throw new NullPointerException("'evenement' ne peut être null.");
		}

		if (!enabled) {
			LOGGER.info(String.format("Evénements fiscaux désactivés : l'événement n°%d n'est pas envoyé.", evenement.getId()));
			return;
		}

		// dispatching sur tous les senders
		for (EvenementFiscalSender sender : senders) {
			sender.sendEvent(evenement);
		}
	}
}
