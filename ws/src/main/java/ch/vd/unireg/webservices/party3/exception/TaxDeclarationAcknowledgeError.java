package ch.vd.unireg.webservices.party3.exception;

import ch.vd.unireg.webservices.party3.TaxDeclarationAcknowledgeCode;

public class TaxDeclarationAcknowledgeError extends Exception {

	private static final long serialVersionUID = 1L;

	private final TaxDeclarationAcknowledgeCode code;

	public TaxDeclarationAcknowledgeError(TaxDeclarationAcknowledgeCode code, String message) {
		super(message);
		if (!code.name().startsWith("ERROR")) {
			throw new IllegalArgumentException();
		}
		this.code = code;
	}

	public TaxDeclarationAcknowledgeCode getCode() {
		return code;
	}
}
