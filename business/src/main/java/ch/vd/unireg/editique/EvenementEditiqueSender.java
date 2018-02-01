package ch.vd.unireg.editique;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.xmlbeans.XmlObject;

import ch.vd.editique.unireg.FichierImpression;

/**
 * Interface qui permet d'envoyer des événements à l'éditique.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface EvenementEditiqueSender {

	/**
	 * Envoie un document à l'éditique pour impression. Cet envoi <b>est inclus</b> dans la transaction courante, le message JMS ne sera donc envoyé que lors du commit de la transaction distribuée.
	 *
	 * @param nomDocument    le nom du document
	 * @param typeDocument   le type de document
	 * @param document       le document sous format XML
	 * @param typeFormat     le format du document
	 * @param archive        détermine si le document doit être archivé ou non.
	 * @throws EditiqueException en cas d'exception lors de l'envoi du message
	 * @return le numéro technique du message JMS envoyé
	 */
	String envoyerDocument(String nomDocument, TypeDocumentEditique typeDocument, XmlObject document, FormatDocumentEditique typeFormat, boolean archive) throws EditiqueException;

	/**
	 * Envoie un document à l'éditique pour impression. Cet envoi <b>est inclus</b> dans la transaction courante, le message JMS ne sera donc envoyé que lors du commit de la transaction distribuée.
	 *
	 * @param nomDocument    le nom du document
	 * @param typeDocument   le type de document
	 * @param document       le document sous format XML
	 * @param typeFormat     le format du document
	 * @param archive        détermine si le document doit être archivé ou non.
	 * @throws EditiqueException en cas d'exception lors de l'envoi du message
	 * @return le numéro technique du message JMS envoyé
	 */
	String envoyerDocument(String nomDocument, TypeDocumentEditique typeDocument, FichierImpression document, FormatDocumentEditique typeFormat, boolean archive) throws EditiqueException;

	/**
	 * Envoie un document à l'éditique pour impression. Cet envoi <b>n'est pas inclus</b> dans la transaction courante, le message JMS est donc envoyé immédiatement (même si la transaction est
	 * annulée - <i>rolled back</i> - ensuite)
	 *
	 * @param nomDocument    le nom du document
	 * @param typeDocument   le type de document
	 * @param document       le document sous format XML
	 * @param typeFormat     le format du document
	 * @param archive        détermine si le document doit être archivé ou non.    @return l'id du message JMS envoyé
	 * @throws EditiqueException en cas d'exception lors de l'envoi du message
	 * @return le numéro technique du message JMS envoyé
	 */
	String envoyerDocumentImmediatement(String nomDocument, TypeDocumentEditique typeDocument, XmlObject document, FormatDocumentEditique typeFormat, boolean archive) throws EditiqueException;

	/**
	 * Envoie un document à l'éditique pour impression. Cet envoi <b>n'est pas inclus</b> dans la transaction courante, le message JMS est donc envoyé immédiatement (même si la transaction est
	 * annulée - <i>rolled back</i> - ensuite)
	 *
	 * @param nomDocument    le nom du document
	 * @param typeDocument   le type de document
	 * @param document       le document sous format XML
	 * @param typeFormat     le format du document
	 * @param archive        détermine si le document doit être archivé ou non.    @return l'id du message JMS envoyé
	 * @throws EditiqueException en cas d'exception lors de l'envoi du message
	 * @return le numéro technique du message JMS envoyé
	 */
	String envoyerDocumentImmediatement(String nomDocument, TypeDocumentEditique typeDocument, FichierImpression document, FormatDocumentEditique typeFormat, boolean archive) throws EditiqueException;

	/**
	 * Envoie une demande d'obtention de copie conforme préalablement archivée. Cet envoi <b>n'est pas inclus</b> dans la transaction courante, le message JMS est donc envoyé immédiatement
	 * (donc même si la transaction est annulée - <i>rolled back</i> - ensuite)
	 *
	 * @param cleArchivage  la clé d'archivage du document
	 * @param typeDocument  le type de document
	 * @param noTiers       le numéro du tiers concerné par le document
	 * @throws EditiqueException en cas d'exception lors de l'envoi du message
	 * @return un couple composé, à gauche, du numéro technique du message JMS envoyé, et à droite du business id (= nom du document)
	 */
	Pair<String, String> envoyerDemandeCopieConforme(String cleArchivage, TypeDocumentEditique typeDocument, long noTiers) throws EditiqueException;
}