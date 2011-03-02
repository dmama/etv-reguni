package ch.vd.uniregctb.inbox;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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
	List<InboxElement> getContent(String visa);

	/**
	 * Ajoute à l'inbox de l'utilisateur le document donné
	 *
	 *
	 * @param visa visa de l'utilisateur dans l'inbox duquel le document doit être ajouté
	 * @param docName nom associé au document
	 * @param description
	 *@param mimeType MIME-type du document à ajouter
	 * @param document contenu du document
	 * @param hoursUntilExpiration la durée minimale (en heures) pendant laquelle le document doit être conservé (0 pour une conservation jusqu'au prochain arrêt de l'application)    @throws java.io.IOException en cas de problème d'accès au document
	 */
	void addDocument(String visa, String docName, String description, String mimeType, InputStream document, int hoursUntilExpiration) throws IOException;
}
