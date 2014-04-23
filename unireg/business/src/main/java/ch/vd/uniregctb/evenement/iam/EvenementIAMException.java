package ch.vd.uniregctb.evenement.iam;

import ch.vd.uniregctb.jms.EsbBusinessCode;

/**
 * Exception métier liée à un événement IAM.
 */
public class EvenementIAMException extends Exception {

	private final EsbBusinessCode businessCode;

	private static final long serialVersionUID = -7641694999256897960L;

	public EvenementIAMException(EsbBusinessCode businessCode, Throwable throwable) {
		super(throwable);
		this.businessCode = businessCode;
	}

	public EvenementIAMException(EsbBusinessCode businessCode, String message) {
		super(message);
		this.businessCode = businessCode;
	}

	public EvenementIAMException(EsbBusinessCode businessCode, String message, Throwable cause) {
		super(message, cause);
		this.businessCode = businessCode;
	}

	public EsbBusinessCode getBusinessCode() {
		return businessCode;
	}
}