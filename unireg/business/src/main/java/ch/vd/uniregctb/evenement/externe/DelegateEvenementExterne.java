package ch.vd.uniregctb.evenement.externe;

/**
 * Interface de délégation du service d'événement externe.
 *
 * @author xcicfh (last modified by $Author: $ @ $Date: $)
 * @version $Revision: $
 */
public interface DelegateEvenementExterne {

    /**
     * cette méthode est appelé quand un événemnt est recu.
     *
     * @param resultat contient les informations de l'évenement.
     * @throws Exception exception
     */
    void surEvenementRecu(EvenementExterneResultat resultat) throws Exception;
}
