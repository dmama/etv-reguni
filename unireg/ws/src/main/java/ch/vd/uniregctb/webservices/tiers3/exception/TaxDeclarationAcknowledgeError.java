package ch.vd.uniregctb.webservices.tiers3.exception;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.webservices.tiers3.TaxDeclarationAcknowledgeCode;

public class TaxDeclarationAcknowledgeError extends Exception {

	private static final long serialVersionUID = 1L;

	private final TaxDeclarationAcknowledgeCode code;

	public TaxDeclarationAcknowledgeError(TaxDeclarationAcknowledgeCode code, String message) {
		super(message);
		Assert.isTrue(code.name().startsWith("ERROR"));
		this.code = code;
	}

	public TaxDeclarationAcknowledgeCode getCode() {
		return code;
	}
}
