package ch.vd.uniregctb.editique.impl;

import ch.vd.uniregctb.editique.EditiqueResultatErreur;

public final class EditiqueResultatErreurImpl extends BaseEditiqueResultatImpl implements EditiqueResultatErreur {

	private final String error;

	public EditiqueResultatErreurImpl(String idDocument, String error) {
		super(idDocument);
		this.error = error;
	}

	@Override
	public String getError() {
		return error;
	}

	@Override
	public String getToStringComplement() {
		return String.format("error='%s'", error);
	}
}
