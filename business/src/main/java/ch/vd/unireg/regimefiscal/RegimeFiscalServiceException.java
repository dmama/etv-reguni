package ch.vd.unireg.regimefiscal;

/**
 * @author Raphaël Marmier, 2017-01-25, <raphael.marmier@vd.ch>
 */
public class RegimeFiscalServiceException extends RuntimeException {

	public RegimeFiscalServiceException() {
		super();
	}

	public RegimeFiscalServiceException(String message) throws RegimeFiscalServiceException {
		super(message);
	}

	public RegimeFiscalServiceException(String message, Throwable t) throws RegimeFiscalServiceException {
		super(message, t);
	}
}
