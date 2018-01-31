package ch.vd.uniregctb.documentfiscal;

/**
 * Exception lancée par le service des autres documents fiscaux en cas de souci
 */
public class AutreDocumentFiscalException extends Exception {

	public AutreDocumentFiscalException(String message) {
		super(message);
	}

	public AutreDocumentFiscalException(String message, Throwable cause) {
		super(message, cause);
	}

	public AutreDocumentFiscalException(Throwable cause) {
		super(cause);
	}
}
