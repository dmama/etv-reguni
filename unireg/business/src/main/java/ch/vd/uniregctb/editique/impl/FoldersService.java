package ch.vd.uniregctb.editique.impl;


/**
 * Interface du service Folders.
 *
 * @author xcifwi (last modified by $Author: xcifwi $ @ $Date: 2007/01/16 10:49:36 $)
 * @version $Revision: 1.3 $
 */
public interface FoldersService {

    /** Constante de format de document. */
    String PDF_FORMAT = "PDF";

    /**
     * Retourne un document pdf, sous forme binaire, identifi� par les diff�rents param�tres.
     *
     * @param typeDossierContribuable le type de dossier contribuable.
     * @param noContribuable le num�ro du contribuable.
     * @param typeDocument le type de document.
     * @param nomDocument le nom du document.
     * @param format le format du document.
     * @return un document pdf, sous forme binaire.
     */
    byte[] getDocument(String typeDossierContribuable, String noContribuable, String typeDocument, String nomDocument,
            String format);

}
