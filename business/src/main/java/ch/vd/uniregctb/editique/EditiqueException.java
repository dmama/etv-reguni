package ch.vd.uniregctb.editique;

/**
 * Exception métier liée au Editique.
 *
 * @author xcifwi (last modified by $Author: xcifwi $ @ $Date: 2007/07/30 08:08:40 $)
 * @version $Revision: 1.1 $
 */
public class EditiqueException extends Exception {

    /** Serial Version UID. */
    private static final long serialVersionUID = 5007516544777492155L;

    /**
     * Constructeur.
     *
     * @param message message expliquant le problème.
     */
    public EditiqueException(String message) {
        super(message);
    }

    public EditiqueException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructeur.
     *
     * @param message message expliquant le problème.
     * @param cause exception source.
     */
    public EditiqueException(String message, Throwable cause) {
        super(message, cause);
    }
}
