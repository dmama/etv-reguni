package ch.vd.uniregctb.evenement.identification.contribuable;

import org.springframework.transaction.annotation.Transactional;

public interface IdentificationContribuableMessageHandler {

	/**
	 * Envoie le message spécifié qui doit contenir la réponse voulue.
	 *
	 * @param message
	 *            le message à envoyer
	 */
	@Transactional(rollbackFor = Throwable.class)
	void sendReponse(IdentificationContribuable message) throws Exception;
}
