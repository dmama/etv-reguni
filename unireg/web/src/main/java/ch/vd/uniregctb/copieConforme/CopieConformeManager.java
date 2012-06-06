package ch.vd.uniregctb.copieConforme;

import java.io.InputStream;

import ch.vd.uniregctb.editique.EditiqueException;

/**
 * Manager des copies conformes de documents archivés dans Folders
 */
public interface CopieConformeManager {

	/**
	 * Renvoie un document PDF de la copie conforme de la sommation de déclaration dont l'état (de type 'SOMME') est indiqué en paramètre
	 *
	 * @return document PDF
	 */
	InputStream getPdfCopieConformeSommation(Long idEtatSomme) throws EditiqueException;

	/**
	 * Renvoie un document PDF de la copie conforme de la confirmation de délai pour une déclaration
	 *
	 * @param idDelai
	 * @return document PDF
	 */
	InputStream getPdfCopieConformeDelai(Long idDelai) throws EditiqueException;

	/**
	 * Renvoie un document PDF identifié par sa clé d'archivage dans Folders
	 * @param noCtb numéro du contribuable pour lequel on va chercher un document
	 * @param key la clé d'archivage
	 * @return document PDF
	 */
	InputStream getPdfCopieConforme(long noCtb, String key) throws EditiqueException;
}
