package ch.vd.uniregctb.jms;

import ch.vd.technical.esb.ErrorType;

/**
 * Exception lancée par un {@link EsbMessageHandler} dans le cas où l'erreur est une erreur métier
 * qui ne doit pas partir en DLQ mais en queue d'erreur
 */
public class EsbBusinessException extends Exception {

	private static final long serialVersionUID = 6575212985364728595L;

	private final EsbBusinessCode code;

	public EsbBusinessException(EsbBusinessCode code, String description, Throwable cause) {
		super(description, cause);
		this.code = code;
	}

	public EsbBusinessException(EsbBusinessCode code, Throwable cause) {
		super(cause);
		this.code = code;
	}

	public ErrorType getErrorType() {
		return code.getType();
	}

	public String getErrorCode() {
		return code.getCode();
	}

	public String getLibelle() {
		return code.getLibelle();
	}
}
