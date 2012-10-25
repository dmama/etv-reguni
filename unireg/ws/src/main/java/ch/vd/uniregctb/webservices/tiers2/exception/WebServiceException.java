package ch.vd.uniregctb.webservices.tiers2.exception;

import javax.xml.bind.annotation.XmlType;

/**
 * Classe de base d'une exception levée par le web-service en cas d'impossibilité de satisfaire une requête.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlType(name = "WebServiceException")
public abstract class WebServiceException extends Exception {

	private static final long serialVersionUID = 7431775875621267716L;

	public WebServiceException() {
	}

	public WebServiceException(Throwable cause) {
		super(buildMessage(cause));
	}

	public WebServiceException(String message) {
		super(message);
	}

	public WebServiceException(String message, Throwable cause) {
		super(buildMessage(message, cause));
	}

	public abstract WebServiceExceptionType getType();

	protected static String buildMessage(Throwable cause) {
		String message = cause.getMessage();
		if (message == null) {
			message = cause.getClass().getName();
		}
		return message;
	}

	protected static String buildMessage(String message, Throwable cause) {
		return message + ':' + buildMessage(cause);
	}
}
