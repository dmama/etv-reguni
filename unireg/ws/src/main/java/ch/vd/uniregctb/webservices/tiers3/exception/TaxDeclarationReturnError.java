package ch.vd.uniregctb.webservices.tiers3.exception;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.webservices.tiers3.TaxDeclarationReturnCode;

public class TaxDeclarationReturnError extends Exception {

	private static final long serialVersionUID = 1L;

	private final TaxDeclarationReturnCode code;

	public TaxDeclarationReturnError(TaxDeclarationReturnCode code, String message) {
		super(message);
		Assert.isTrue(code.name().startsWith("ERROR"));
		this.code = code;
	}

	public TaxDeclarationReturnCode getCode() {
		return code;
	}
}
