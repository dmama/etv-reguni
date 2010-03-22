package ch.vd.uniregctb.editique.impl;


/**
 * Interface du service Folders.
 *
 * @author xcifwi (last modified by $Author: xcifwi $ @ $Date: 2007/01/16 10:49:36 $)
 * @version $Revision: 1.3 $
 */
public interface FoldersService {

    /** Constante de format de document. */
    final String PDF_FORMAT = "PDF";

    /**
     * Retourne un document pdf, sous forme binaire, identifié par les différents paramètres.
     *
     * @param typeDossierContribuable le type de dossier contribuable.
     * @param noContribuable le numéro du contribuable.
     * @param typeDocument le type de document.
     * @param nomDocument le nom du document.
     * @param format le format du document.
     * @return un document pdf, sous forme binaire.
     */
    byte[] getDocument(String typeDossierContribuable, String noContribuable, String typeDocument, String nomDocument, String format);

}
