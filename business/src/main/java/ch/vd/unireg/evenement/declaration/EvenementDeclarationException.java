package ch.vd.unireg.evenement.declaration;

import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.jms.EsbBusinessException;

public class EvenementDeclarationException extends EsbBusinessException {

	private static final long serialVersionUID = 6258715960617934016L;

	public EvenementDeclarationException(EsbBusinessCode businessCode, String message) {
		super(businessCode, message, null);
	}

	public EvenementDeclarationException(EsbBusinessCode businessCode, String message, Throwable cause) {
		super(businessCode, message, cause);
	}

	public EvenementDeclarationException(EsbBusinessCode businessCode, Throwable cause) {
		super(businessCode, cause);
	}
}
