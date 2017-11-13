package ch.vd.uniregctb.copieConforme;

import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.TypeDocumentEditique;

/**
 * Manager des copies conformes de documents archivés dans Folders
 */
public interface CopieConformeManager {

	/**
	 * Renvoie un document PDF de la copie conforme de la sommation de déclaration dont l'état (de type "SOMME") est indiqué en paramètre
	 * @param idEtatSomme identifiant de l'état "sommé" de la sommation (DI ou LR)
	 * @return document PDF
	 */
	EditiqueResultat getPdfCopieConformeSommation(Long idEtatSomme) throws EditiqueException;

	/**
	 * Renvoie un document PDF de la copie conforme de la sommation de document dont l'état (de type "RAPPELE") est indiqué en paramètre
	 * @param idEtatRappele identifiant de l'état "rappelé" de la sommation (questionnaire SNC)
	 * @return document PDF
	 */
	EditiqueResultat getPdfCopieConformeRappel(Long idEtatRappele) throws EditiqueException;

	/**
	 * Renvoie un document PDF de la copie conforme de la confirmation de délai pour une déclaration
	 * @param idDelai identifiant du délai accordé pour le renvoi de la déclaration
	 * @return document PDF
	 */
	EditiqueResultat getPdfCopieConformeDelai(Long idDelai) throws EditiqueException;

	/**
	 * Renvoie un document PDF de la copie conforme de l'envoi initial de l'autre document fiscal dont l'identifiant est passé en paramètre
	 * @param idDocument identifiant du document
	 * @return document PDF
	 * @throws EditiqueException en cas de problème
	 */
	EditiqueResultat getPdfCopieConformeEnvoiAutreDocumentFiscal(Long idDocument) throws EditiqueException;

	/**
	 * Renvoie un document PDF de la copie conforme du rappel de l'autre document fiscal dont l'identifiant est passé en paramètre
	 * @param idDocument identifiant du document
	 * @return document PDF
	 * @throws EditiqueException en cas de problème
	 */
	EditiqueResultat getPdfCopieConformeRappelAutreDocumentFiscal(Long idDocument) throws EditiqueException;

	/**
	 * Renvoie un document PDF identifié par sa clé d'archivage dans Folders
	 * @param noCtb numéro du contribuable pour lequel on va chercher un document
	 * @param typeDoc le type de document (voir {@link ch.vd.uniregctb.editique.TypeDocumentEditique})
	 * @param key la clé d'archivage
	 * @return document PDF
	 */
	EditiqueResultat getPdfCopieConforme(long noCtb, TypeDocumentEditique typeDoc, String key) throws EditiqueException;
}
