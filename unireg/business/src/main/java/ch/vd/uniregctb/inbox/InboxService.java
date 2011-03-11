package ch.vd.uniregctb.inbox;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Interface du service d'inbox (sorte de boîte aux lettres dans laquelle sont rassemblés les documents
 * reçus de manière asynchrone pour un utilisateur donné).<p/>
 * Cette inbox n'est pas persistée entre deux démarrages de l'application.
 */
public interface InboxService {

	/**
	 * @param visa visa de l'utilisateur qui veut connaître le contenu de son inbox
	 * @return le contenu de l'inbox de l'utilisateur identifié par son visa, trié par ordre inverse de l'ordre d'arrivée
	 */
	List<InboxElement> getInboxContent(String visa);

	/**
	 * @param uuid identifiant du message à récupérer
	 * @return le message trouvé correspondant à l'indentifiant indiqué, ou <code>null</code> si le document est absent (jamais vu ou expiré)
	 */
	InboxElement getInboxElement(UUID uuid);

	/**
	 * Ajoute à l'inbox de l'utilisateur le document donné
	 *
	 * @param visa visa de l'utilisateur dans l'inbox duquel le document doit être ajouté
	 * @param docName nom associé au document
	 * @param description petite description associée au document
	 * @param attachment (optionel) attachement au document reçu
	 * @param hoursUntilExpiration la durée minimale (en heures) pendant laquelle le document doit être conservé (0 pour une conservation jusqu'au prochain arrêt de l'application)
	 * @throws java.io.IOException en cas de problème d'accès au document  @throws IOException en cas d'erreur à la lecture du document donné
	 */
	void addDocument(String visa, String docName, String description, InboxAttachment attachment, int hoursUntilExpiration) throws IOException;

	/**
	 * Ajoute à l'inbox de l'utilisateur le document donné
	 * @param uuid identifiant unique de document
	 * @param visa visa de l'utilisateur dans l'inbox duquel le document doit être ajouté
	 * @param docName nom associé au document
	 * @param description petite description associée au document
	 * @param attachment (optionel) attachement au document reçu
	 * @param hoursUntilExpiration la durée minimale (en heures) pendant laquelle le document doit être conservé (0 pour une conservation jusqu'au prochain arrêt de l'application)    @throws java.io.IOException en cas de problème d'accès au document
	 * @throws IOException en cas d'erreur à la lecture du document donné
	 */
	void addDocument(UUID uuid, String visa, String docName, String description, InboxAttachment attachment, int hoursUntilExpiration) throws IOException;
}
