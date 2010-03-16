package ch.vd.uniregctb.evenement.externe;



/**
 * Représente le résultat retourné par le service des événements impôt source.
 *
 * @author xcicfh (last modified by $Author: $ @ $Date: $)
 * @version $Revision: $
 */
public interface EvenementExterneResultat {

    /**
     * Obtient l'évenement externe de la réponse.
     *
     * @return Retourne l'évenement externe de la réponse.
     */
    IEvenementExterne getEvenement();


    /**
     * Obtient l'emmetteur de l'événement.
     * @return Retourne l'emmetteur de l'événement.
     */
    EmmetteurType getEmmetteur();

    /**
     * Obtient l'indication si une erreur est survenue.
     *
     * @return <code>true</code> si une erreur est survenue, autrement <code>false</code>.
     * @see #getError()
     */
    boolean hasError();

    /**
     * Obtient le message de l'erreur survenue lors de la réception de l'événement.
     *
     * @return Retourn l'exception en cas d'erreur, sinon <codeb>null</code>.
     */
    Exception getError();

    /**
     * Obtient le texte de la réponse.
     *
     * @return Retourne le texte de la réponse.
     */
    String getText();

    /**
     * Obtient l'idenfiant unique du message
     * @return
     */
    String getCorrelationId();
}
