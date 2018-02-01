package ch.vd.unireg.editique.print;

public class PrintPCLException extends Exception {

	private static final long serialVersionUID = -8346595753203629233L;

	/**
     * Constructeur.
     *
     * @param message message expliquant le problème.
     */
    public PrintPCLException(String message) {
        super(message);
    }

    public PrintPCLException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructeur.
     *
     * @param message message expliquant le problème.
     * @param cause exception source.
     */
    public PrintPCLException(String message, Throwable cause) {
        super(message, cause);
    }

}
