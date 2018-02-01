package ch.vd.unireg.evenement.retourdi.pp;

import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.jms.EsbBusinessException;

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