package ch.vd.uniregctb.evenement.organisation;

import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbBusinessException;

/**
 * Exception lancée par la réception des événements organisation
 */
public class EvenementOrganisationEsbException extends EsbBusinessException {

	private static final long serialVersionUID = 8891159928896424726L;

	public EvenementOrganisationEsbException(EsbBusinessCode businessCode, Throwable cause) {
		super(businessCode, cause);
	}

	public EvenementOrganisationEsbException(EsbBusinessCode businessCode, String message) {
		super(businessCode, message, null);
	}
}
