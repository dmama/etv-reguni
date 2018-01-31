package ch.vd.uniregctb.editique.impl;

import ch.vd.technical.esb.ErrorType;
import ch.vd.uniregctb.editique.EditiqueResultatTimeout;

public class EditiqueResultatTimeoutImpl extends BaseEditiqueResultatImpl implements EditiqueResultatTimeout {

	public EditiqueResultatTimeoutImpl(String idDocument) {
		super(idDocument);
	}

	@Override
	public String getErrorMessage() {
		return "Time-out";
	}

	@Override
	public ErrorType getErrorType() {
		return ErrorType.TECHNICAL;
	}

	@Override
	public Integer getErrorCode() {
		return 504;       // HTTP Gateway timeout (see http://en.wikipedia.org/wiki/List_of_HTTP_status_codes)
	}

	@Override
	protected String getToStringComplement() {
		return null;
	}
}
