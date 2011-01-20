package ch.vd.uniregctb.evenement.identification.contribuable;

import org.springframework.transaction.annotation.Transactional;

public interface IdentificationContribuableMessageHandler {

	/**
	 * Défini le handler qui sera appelé lors de la réception d'une demande d'identification de contribuable. Le handler est responsable
	 * d'entreprendre toutes les actions <i>métier</i> nécessaires au traitement correct du message.
	 *
	 * @param handler
	 *            le handler <i>métier</i> de demande d'identification de contribuable
	 */
	void setDemandeHandler(DemandeHandler handler);

	/**
	 * Envoie le message spécifié qui doit contenir la réponse voulue.
	 *
	 * @param message
	 *            le message à envoyer
	 */
	@Transactional(rollbackFor = Throwable.class)
	void sendReponse(IdentificationContribuable message) throws Exception;
}
