package ch.vd.uniregctb.editique;

import java.io.InputStream;

import org.apache.xmlbeans.XmlObject;

import ch.vd.editique.service.enumeration.TypeFormat;

/**
 * Service Editique. Ce service est dédié à la communication avec le service Editique permettant l'impression des divers documents.
 *
 * @author xcifwi (last modified by $Author: xcicfh $ @ $Date: 2007/08/15 06:14:15 $)
 * @version $Revision: 1.8 $
 */
public interface EditiqueService {

	/**
	 * Sérialise au format XML et transmet l'object en paramètre au service Editique JMS d'impression directe.
	 *
	 * @param nomDocument  le nom du document à transmettre à Editique.
	 * @param typeDocument le type de document
	 * @param typeFormat   le format souhaité
	 * @param document
	 *@param archive      indicateur d'archivage  @return le document imprimé ou <b>null</b> si éditique n'a pas répondu dans les temps
	 * @throws EditiqueException si un problème survient durant la génération du XML ou durant la transmission du message au serveur JMS.
	 */
	EditiqueResultat creerDocumentImmediatement(String nomDocument, String typeDocument, TypeFormat typeFormat, XmlObject document, boolean archive) throws EditiqueException;

	/**
	 * Sérialise au format XML et transmet l'object en paramètre au service Editique JMS d'impression de masse.
	 *
	 * @param nomDocument
	 * @param typeDocument le type de document
	 * @param document
	 * @param archive      indicateur d'archivage   @throws EditiqueException si un problème survient durant la génération du XML ou durant la transmission du message au serveur JMS.
	 */
	void creerDocumentParBatch(String nomDocument, String typeDocument, XmlObject document, boolean archive) throws EditiqueException;

	/**
	 * Obitent un document pdf, sous forme binaire, identifié par les différents paramètres.
	 *
	 * @param noContribuable l'identifiant du contribuable.
	 * @param typeDocument   le type de document.
	 * @param nomDocument    le nom du document.
	 * @param contexte       token identifiant le contexte de récupération de ce document depuis l'archive
	 * @return un document pdf, sous forme binaire.
	 * @throws EditiqueException si un problème survient durant la génération du XML ou durant la transmission du message au serveur JMS.
	 */
	InputStream getPDFDeDocumentDepuisArchive(Long noContribuable, String typeDocument, String nomDocument, String contexte) throws EditiqueException;

}
