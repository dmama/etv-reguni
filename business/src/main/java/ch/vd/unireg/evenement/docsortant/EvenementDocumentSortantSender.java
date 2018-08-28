package ch.vd.unireg.evenement.docsortant;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.xml.event.docsortant.v1.Documents;

/**
 * Service d'envoi dans l'ESB des messages déclarant l'envoi d'un document sortant
 */
public interface EvenementDocumentSortantSender {

	/**
	 * Envoi un événement vers l'ESB
	 *
	 * @param businessId businessId du message ESB
	 * @param docs les documents à envoyer
	 * @param reponseAttendue <code>true</code> si on doit demander une réponse (avec l'ID d'indexation)
	 * @param additionalHeaders (optional) map des headers à ajouter à l'événement en sortie
	 * @param local Indique si c'est un traitement local ou de masse dont les documents doivent être diriger vers une destination adaptée
	 * @throws EvenementDocumentSortantException en cas de souci
	 */
	void sendEvenementDocumentSortant(String businessId, Documents docs, boolean reponseAttendue, @Nullable Map<String, String> additionalHeaders, boolean local) throws EvenementDocumentSortantException;
}
