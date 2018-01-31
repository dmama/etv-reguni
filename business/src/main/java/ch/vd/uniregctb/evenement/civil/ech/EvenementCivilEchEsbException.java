package ch.vd.uniregctb.evenement.civil.ech;

import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbBusinessException;

/**
 * Exception lancée par la réception des événements civils
 */
public class EvenementCivilEchEsbException extends EsbBusinessException {

	private static final long serialVersionUID = 6907310973448208150L;

	public EvenementCivilEchEsbException(EsbBusinessCode businessCode, Throwable cause) {
		super(businessCode, cause);
	}

	public EvenementCivilEchEsbException(EsbBusinessCode businessCode, String message) {
		super(businessCode, message, null);
	}
}
