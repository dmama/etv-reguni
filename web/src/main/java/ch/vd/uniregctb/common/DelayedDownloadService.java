package ch.vd.uniregctb.common;

import java.io.IOException;
import java.util.UUID;

import ch.vd.uniregctb.editique.EditiqueResultatDocument;

/**
 * Service d'accès au données des téléchargements décalés (en ce sens que les données à télécharger
 * sont stockées temporairement et récupérer un peu plus tard après le chargement d'une nouvelle page)
 */
public interface DelayedDownloadService {

	String SESSION_ATTRIBUTE_NAME = "delayedDownloadId";

	/**
	 * @param document document à stocker temporairement
	 * @param filenameRadical radical du nom du fichier à utiliser lors du téléchargement
	 * @return identifiant du document stocké
	 */
	UUID putDocument(EditiqueResultatDocument document, String filenameRadical) throws IOException;

	/**
	 * Récupération du document précédemment placé là, au travers de l'identifiant généré alors
	 * @param id identifiant du document, fourni lors de l'insertion du document
	 * @param remove si <code>true</code>, retire le document de la map avant de le retourner
	 * @return le document identifié par son identifiant
	 */
	TypedDataContainer fetchDocument(UUID id, boolean remove);

	/**
	 * Destruction du document précédemment placé là
	 * @param id identifiant du document, founi lors de l'insertion du document
	 */
	void eraseDocument(UUID id);

	/**
	 * @return le nombre de document enregistrés mais pas récupérés
	 */
	int getPendingSize();
}
