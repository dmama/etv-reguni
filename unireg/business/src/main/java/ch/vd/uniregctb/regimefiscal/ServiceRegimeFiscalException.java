package ch.vd.uniregctb.regimefiscal;

/**
 * @author Raphaël Marmier, 2017-01-25, <raphael.marmier@vd.ch>
 */
public class ServiceRegimeFiscalException extends RuntimeException {

	public ServiceRegimeFiscalException() {
		super();
	}

	public ServiceRegimeFiscalException(String message) throws ServiceRegimeFiscalException {
		super(message);
	}

	public ServiceRegimeFiscalException(String message, Throwable t) throws ServiceRegimeFiscalException {
		super(message, t);
	}
}
