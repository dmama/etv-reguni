package ch.vd.uniregctb.evenement.retourdi.pp;

import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbBusinessException;

/**
 * Exception métier liée à un événement CEDI.
 */
public class EvenementCediException extends EsbBusinessException {

	private static final long serialVersionUID = 7640140212225870202L;

	public EvenementCediException(EsbBusinessCode businessCode, String message) {
		super(businessCode, message, null);
	}

	public EvenementCediException(EsbBusinessCode businessCode, String message, Exception cause) {
		super(businessCode, message, cause);
	}
}