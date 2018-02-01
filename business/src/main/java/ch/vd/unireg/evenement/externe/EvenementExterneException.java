package ch.vd.uniregctb.evenement.externe;


/**
 * Exception métier liée au événement externe.
 *
 * @author xcicfh (last modified by $Author: $ @ $Date: $)
 * @version $Revision: $
 */
public class EvenementExterneException extends Exception {

    /** Serial Version UID. */
    private static final long serialVersionUID = 5007516544777492155L;

    /**
     * Constructeur.
     *
     * @param message message expliquant le problème.
     */
    public EvenementExterneException(String message) {
        super(message);
    }

    /**
     * Constructeur.
     *
     * @param message message expliquant le problème.
     * @param cause exception source.
     */
    public EvenementExterneException(String message, Throwable cause) {
        super(message, cause);
    }
}
