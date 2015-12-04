package ch.vd.uniregctb.document;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

import org.springframework.transaction.annotation.Transactional;

/**
 * Service de gestion des documents internes à Unireg
 */
@Transactional(rollbackFor = Throwable.class)
public interface DocumentService {

	/**
	 * Crée un nouveau document.
	 *
	 * @param <T>
	 *            le type de document voulu
	 * @param clazz
	 *            la classe correspondant au type voulu
	 * @param nom
	 *            le nom "utilisateur" du document
	 * @param description
	 *            une description du document, peut être <b>null</b>
	 * @param fileExtension
	 *            l'extension du fichier sur le disque
	 * @param callback
	 *            la méthode de remplissage du document
	 * @return la référence du nouveau document
	 */
	public <T extends Document> T newDoc(Class<T> clazz, String nom, String description, String fileExtension, WriteDocCallback<T> callback) throws Exception;

	/**
	 * Lit le contenu d'un document.
	 *
	 * @param <T>
	 *            le type de document voulu
	 * @param doc
	 *            le document considéré
	 * @param callback
	 *            la méthode de remplissage du document
	 */
	public <T extends Document> void readDoc(T doc, ReadDocCallback<T> callback) throws Exception;

	/**
	 * @return retourne le document correspondant à l'id spécifié.
	 */
	public Document get(Long id) throws Exception;

	/**
	 * Efface le document spécifié de l'index (database) et du filesystem.
	 */
	public void delete(Document doc) throws Exception;

	/**
	 * @return la collection de tous les documents non-annulés.
	 */
	public Collection<Document> getDocuments() throws Exception;

	/**
	 * Scanne le repository des documents à la recherche de fichiers non-référencés et référence ceux-ci dans la base de données.
	 *
	 * @return la liste des documents ramassés.
	 */
	public Collection<Document> ramasseDocs();

	/**
	 * @return la collection de tous les documents non-annulés du type spécifié.
	 */
	public <T extends Document> Collection<T> getDocuments(Class<T> clazz) throws Exception;

	public interface WriteDocCallback<T> {
		void writeDoc(T doc, OutputStream os) throws Exception;
	}

	public interface ReadDocCallback<T> {
		void readDoc(T doc, InputStream is) throws Exception;
	}
}
