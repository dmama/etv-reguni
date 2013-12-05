package ch.vd.uniregctb.webservices.tiers2.exception;

/**
 * Exception levée par le web-service lorsqu'une erreur interne au web-service est levée. Dans ce cas, il s'agit généralement d'un bug du
 * web-service.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class TechnicalException extends WebServiceException {

	private static final long serialVersionUID = 7021233813940387848L;

	public TechnicalException(Throwable cause) {
		super(cause);
	}

	public TechnicalException(String message) {
		super(message);
	}

	public TechnicalException(String message, Throwable cause) {
		super(message, cause);
	}

	@Override
	public WebServiceExceptionType getType() {
		return WebServiceExceptionType.TECHNICAL;
	}
}
