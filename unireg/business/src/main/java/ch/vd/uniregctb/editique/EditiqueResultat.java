package ch.vd.uniregctb.editique;

/**
 * Représente le résultat retourné par Editique suite à une demande de création de document.
 *
 * @see EditiqueService#creerDocumentImmediatement(String,Object)
 *
 * @author xcicfh (last modified by $Author: xcicfh $ @ $Date: 2007/09/13 11:20:03 $)
 * @version $Revision: 1.2 $
 */
public interface EditiqueResultat {

	/**
	 * Obtient une mime représentation du type de fichier retourné.
	 *
	 * @return une mime représentation du type de fichier retourné.
	 * @see #getDocument()
	 */
	String getContentType();

	/**
	 * Obtient le contenu du document.
	 *
	 * @return le contenu du document.
	 */
	byte[] getDocument();

	/**
	 * Obtient le message de l'erreur survenue lors de la création du document.
	 *
	 * @return le message en cas d'erreur, sinon <codeb>null</code>.
	 */
	String getError();

	/**
	 * Obtient le type de document.
	 *
	 * @return le type de document.
	 */
	String getDocumentType();

	/**
	 * @return la chaîne de caractère qui identifie un document de manière unique à travers une requête à éditique et la réponse d'éditique.
	 */
	String getIdDocument();

	/**
	 * Obtient l'indication si une erreur est survenue.
	 *
	 * @return <code>true</code> si une erreur est survenue, autrement <code>false</code>.
	 * @see #getError()
	 */
	boolean hasError();

	/**
	 * Obtient le timestamp de la réception du document.
	 *
	 * @return Retourne le timestamp de la réception du document.
	 */
	long getTimestampReceived();
}
