package ch.vd.unireg.evenement.fiscal;


/**
 * Exception métier liée au événement fiscal.
 *
 * @author xcifwi (last modified by $Author: $ @ $Date: $)
 * @version $Revision: $
 */
public class EvenementFiscalException extends RuntimeException {

    /** Serial Version UID. */
    private static final long serialVersionUID = 5007516544777492155L;

    /**
     * Constructeur.
     *
     */
    public EvenementFiscalException() {
        super();
    }

    /**
     * Constructeur.
     *
     * @param message message expliquant le problème.
     */
    public EvenementFiscalException(String message) {
        super(message);
    }

	public EvenementFiscalException(String message, Throwable cause) {
		super(message, cause);
	}
}
