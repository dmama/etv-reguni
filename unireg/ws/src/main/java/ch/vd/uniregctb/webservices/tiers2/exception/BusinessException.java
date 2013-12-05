package ch.vd.uniregctb.webservices.tiers2.exception;

/**
 * Exception levée par le web-service lorsqu'une erreur métier est levée.
 * <p>
 * Une erreur métier peut être le résultat de paramètres d'entrée incorrects, de données incohérentes dans la base, de problèmes de
 * connectivités avec d'autres registres, etc...
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class BusinessException extends WebServiceException {

	private static final long serialVersionUID = -5654310705856240195L;

	public BusinessException(Throwable cause) {
		super(cause);
	}

	public BusinessException(String message) {
		super(message);
	}

	public BusinessException(String message, Throwable cause) {
		super(message, cause);
	}

	@Override
	public WebServiceExceptionType getType() {
		return WebServiceExceptionType.BUSINESS;
	}
}
