package ch.vd.uniregctb.editique;

import java.io.InputStream;

import org.apache.xmlbeans.XmlObject;

import ch.vd.editique.service.enumeration.TypeFormat;

/**
 * Service Editique. Ce service est dédié à la communication avec le service Editique permettant l'impression des divers documents.
 */
public interface EditiqueService {

	/**
	 * Sérialise au format XML et transmet l'object en paramètre au service Editique JMS d'impression directe. Si le délai de réponse éditique
	 * est dépassé (voir variable editique.locale.sync.attente.timeout), la méthode renvoie un objet qui implémente {@link EditiqueResultatTimeout}
	 * et le retour d'impression, s'il arrive un jour, sera poubellisé.
	 *
	 * @param nomDocument  le nom du document à transmettre à Editique.
	 * @param typeDocument le type de document
	 * @param typeFormat   le format souhaité
	 * @param document     document XML à envoyer à éditique
	 * @param archive      indicateur d'archivage
	 * @return le document imprimé ou <b>null</b> si éditique n'a pas répondu dans les temps
	 * @throws EditiqueException si un problème survient durant la génération du XML ou durant la transmission du message au serveur JMS.
	 */
	EditiqueResultat creerDocumentImmediatementSynchroneOuRien(String nomDocument, String typeDocument, TypeFormat typeFormat, XmlObject document, boolean archive) throws EditiqueException;

	/**
	 * Sérialise au format XML et transmet l'object en paramètre au service Editique JMS d'impression directe ; si l'impression est un peu lente,
	 * la méthode retourne un objet qui implémente {@link EditiqueResultatReroutageInbox} au bout du temps imparti (défaut : 15 secondes, voir la variable editique.locale.async.attente.delai)
	 * après avoir enregistré une demande de re-routage du résultat d'impression vers l'inbox du demandeur dès qu'il finira par arriver
	 *
	 * @param nomDocument le nom du document à transmettre à Editique.
	 * @param typeDocument le type de document
	 * @param typeFormat le format souhaité
	 * @param document document XML à envoyer à éditique
	 * @param archive indicateur d'archivage
	 * @param description une description textuelle de l'impression, utilisable dans la description du message qui reviendrait par l'inbox
	 * @return le document imprimé ou <b>null</b> si éditique n'a pas répondu dans les temps
	 * @throws EditiqueException si un problème survient durant la génération du XML ou durant la transmission du message au serveur JMS.
	 */
	EditiqueResultat creerDocumentImmediatementSynchroneOuInbox(String nomDocument, String typeDocument, TypeFormat typeFormat, XmlObject document, boolean archive, String description) throws EditiqueException;

	/**
	 * Sérialise au format XML et transmet l'object en paramètre au service Editique JMS d'impression de masse.
	 *
	 * @param nomDocument  le nom du document à transmettre à Editique (identifiant)
	 * @param typeDocument le type de document
	 * @param document     document XML à envoyer à éditique
	 * @param archive      indicateur d'archivage   @throws EditiqueException si un problème survient durant la génération du XML ou durant la transmission du message au serveur JMS.
	 * @throws EditiqueException en cas de problème à l'envoi
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
