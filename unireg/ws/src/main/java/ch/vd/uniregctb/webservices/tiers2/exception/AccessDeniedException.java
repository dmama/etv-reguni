package ch.vd.uniregctb.webservices.tiers2.exception;

/**
 * Exception levée par le web-service lorsque l'accès à une ressource n'est pas autorisé.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class AccessDeniedException extends WebServiceException {

	private static final long serialVersionUID = -8817423170883590884L;

	public AccessDeniedException(String message) {
		super(message);
	}

	@Override
	public WebServiceExceptionType getType() {
		return WebServiceExceptionType.ACCESS_DENIED;
	}
}
