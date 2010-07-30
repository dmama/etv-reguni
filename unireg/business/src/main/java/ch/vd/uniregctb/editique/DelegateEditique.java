package ch.vd.uniregctb.editique;

/**
 * Interface de délégation du service d'Editique,
 *
 * @author xcicfh (last modified by $Author: xciflm $ @ $Date: 2007/09/13 06:36:24 $)
 * @version $Revision: 1.1 $
 */
public interface DelegateEditique {

    /**
     * Méthode appelée quand un document est reçu de Editique.
     *
     * @param resultat contient les informations de la réception du document.
     */
    void surDocumentRecu(EditiqueResultat resultat);
}
