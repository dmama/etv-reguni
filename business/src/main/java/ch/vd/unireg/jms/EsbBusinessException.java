package ch.vd.unireg.jms;

import org.apache.commons.lang3.StringUtils;

/**
 * Exception lancée par un {@link EsbMessageHandler} dans le cas où l'erreur est une erreur métier
 * qui ne doit pas partir en DLQ mais en queue d'erreur
 */
public class EsbBusinessException extends Exception {

	private static final long serialVersionUID = 4877221114973071143L;

	private final EsbBusinessCode code;

	public EsbBusinessException(EsbBusinessCode code, String description, Throwable cause) {
		super(description, cause);
		this.code = code;
	}

	public EsbBusinessException(EsbBusinessCode code, Throwable cause) {
		super(cause);
		this.code = code;
	}

	@Override
	public String getMessage() {
		final String defaultMessage = super.getMessage();
		if (StringUtils.isNotBlank(defaultMessage)) {
			return defaultMessage;
		}

		final String msg;
		Throwable causeWithMessage = getCause();
		while (causeWithMessage != null && StringUtils.isBlank(causeWithMessage.getMessage())) {
			causeWithMessage = causeWithMessage.getCause();
		}
		if (causeWithMessage != null) {
			msg = causeWithMessage.getMessage();
		}
		else if (getCause() != null) {
			msg = getCause().getClass().getName();
		}
		else {
			msg = code.getLibelle();
		}
		return msg;
	}

	public EsbBusinessCode getCode() {
		return code;
	}
}
