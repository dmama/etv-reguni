package ch.vd.uniregctb.webservices.common;

/**
 * Exception générique levée par le web-service en cas d'impossibilité de satisfaire une requête.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public final class WebServiceException extends Exception {

	private static final long serialVersionUID = 7431775875621267716L;

	public WebServiceException() {
	}

	public WebServiceException(Throwable cause) {
		super(cause.getMessage());
	}

	public WebServiceException(String message) {
		super(message);
	}

	public WebServiceException(String message, Throwable cause) {
		super(message + ":" + cause.getMessage());
	}
}
