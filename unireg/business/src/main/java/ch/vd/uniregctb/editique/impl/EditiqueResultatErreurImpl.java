package ch.vd.uniregctb.editique.impl;

import ch.vd.technical.esb.ErrorType;
import ch.vd.uniregctb.editique.EditiqueResultatErreur;

public final class EditiqueResultatErreurImpl extends BaseEditiqueResultatImpl implements EditiqueResultatErreur {

	private final String errorMessage;
	private final ErrorType errorType;
	private final String errorCode;

	public EditiqueResultatErreurImpl(String idDocument, String errorMessage, ErrorType errorType, String errorCode) {
		super(idDocument);
		this.errorMessage = errorMessage;
		this.errorType = errorType;
		this.errorCode = errorCode;
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

	public ErrorType getErrorType() {
		return errorType;
	}

	public String getErrorCode() {
		return errorCode;
	}

	@Override
	public String getToStringComplement() {
		return String.format("error='%s' (%s/%s)", errorMessage, errorType, errorCode);
	}
}
