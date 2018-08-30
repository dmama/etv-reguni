package ch.vd.unireg.evenement.cybercontexte;

import ch.vd.unireg.evenement.declaration.EvenementDeclarationException;
import ch.vd.unireg.jms.EsbBusinessCode;

/**
 * Exception levée lorsqu'une publication dans le cyber-contexte s'est mal passée.
 */
public class EvenementCyberContexteException extends EvenementDeclarationException {

	private static final long serialVersionUID = 9185500326914297581L;

	public EvenementCyberContexteException(EsbBusinessCode businessCode, String message) {
		super(businessCode, message);
	}

	public EvenementCyberContexteException(EsbBusinessCode businessCode, String message, Throwable cause) {
		super(businessCode, message, cause);
	}

	public EvenementCyberContexteException(EsbBusinessCode businessCode, Throwable cause) {
		super(businessCode, cause);
	}
}
