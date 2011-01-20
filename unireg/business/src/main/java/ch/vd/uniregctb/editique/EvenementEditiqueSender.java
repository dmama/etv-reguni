package ch.vd.uniregctb.editique;

import org.apache.xmlbeans.XmlObject;

import ch.vd.editique.service.enumeration.TypeFormat;
import ch.vd.editique.service.enumeration.TypeImpression;

/**
 * Interface qui permet d'envoyer des événements à l'éditique.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface EvenementEditiqueSender {

	/**
	 * Envoie un document à l'éditique pour impression. Cette méthode est incluse dans la transaction courante, le message JMS ne sera donc envoyé que lors du commit de la transaction distribuée.
	 *
	 * @param nomDocument    le nom du document
	 * @param typeDocument   le type de document
	 * @param document       le document sous format XML
	 * @param typeFormat     le format du document
	 * @param archive        détermine si le document doit être archivé ou non.
	 * @return l'id du message JMS envoyé
	 * @throws EditiqueException en cas d'exception lors de l'envoi du message
	 * @return le numéro technique du message JMS envoyé
	 */
	String envoyerDocument(final String nomDocument, final String typeDocument, XmlObject document, TypeFormat typeFormat, boolean archive) throws EditiqueException;

	/**
	 * Envoie un document à l'éditique pour impression. Cette méthode <b>n'est pas incluse</b> dans la transaction courante, le message JMS est donc envoyé immédiatement même si la transaction est
	 * rollée-back par après.
	 *
	 * @param nomDocument    le nom du document
	 * @param typeDocument   le type de document
	 * @param document       le document sous format XML
	 * @param typeFormat     le format du document
	 * @param archive        détermine si le document doit être archivé ou non.    @return l'id du message JMS envoyé
	 * @throws EditiqueException en cas d'exception lors de l'envoi du message
	 * @return le numéro technique du message JMS envoyé
	 */
	String envoyerDocumentImmediatement(final String nomDocument, final String typeDocument, XmlObject document, TypeFormat typeFormat, boolean archive) throws EditiqueException;
}