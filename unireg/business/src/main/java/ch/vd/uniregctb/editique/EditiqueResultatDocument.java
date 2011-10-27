package ch.vd.uniregctb.editique;

/**
 * Interface implémentée par un résultat qui contient un document de retour d'éditique
 */
public interface EditiqueResultatDocument extends EditiqueResultatRecu {

	/**
	 * Obtient une mime représentation du type de fichier retourné.
	 *
	 * @return une mime représentation du type de fichier retourné.
	 * @see #getDocument()
	 */
	String getContentType();

	/**
	 * Obtient le type de document.
	 *
	 * @return le type de document.
	 */
	String getDocumentType();

	/**
	 * Obtient le contenu du document.
	 *
	 * @return le contenu du document.
	 */
	byte[] getDocument();

}
