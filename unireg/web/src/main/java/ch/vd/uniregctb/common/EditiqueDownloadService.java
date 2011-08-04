package ch.vd.uniregctb.common;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import ch.vd.uniregctb.editique.EditiqueResultatDocument;

/**
 * Interface du service qui permet de factoriser la réception et le téléchargement d'un retour
 * d'impression de l'éditique
 */
public interface EditiqueDownloadService {

	/**
	 * Remplit la réponse HTTP avec le contenu du retour d'impression éditique
	 * @param resultat résultat contenant un document à télécharger
	 * @param filenameRadical radical du nom de fichier à présenter dans la réponse HTTP
	 * @param response réponse HTTP à remplir avec le contenu du fichier
	 * @throws java.io.IOException en cas de procblème
	 */
	void download(EditiqueResultatDocument resultat, String filenameRadical, HttpServletResponse response) throws IOException;

}
