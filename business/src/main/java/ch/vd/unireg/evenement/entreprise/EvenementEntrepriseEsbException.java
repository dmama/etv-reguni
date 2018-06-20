package ch.vd.unireg.evenement.entreprise;

import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.jms.EsbBusinessException;

/**
 * Exception lancée par la réception des événements entreprise
 */
public class EvenementEntrepriseEsbException extends EsbBusinessException {

	private static final long serialVersionUID = 8891159928896424726L;

	public EvenementEntrepriseEsbException(EsbBusinessCode businessCode, Throwable cause) {
		super(businessCode, cause);
	}

	public EvenementEntrepriseEsbException(EsbBusinessCode businessCode, String message) {
		super(businessCode, message, null);
	}
}
