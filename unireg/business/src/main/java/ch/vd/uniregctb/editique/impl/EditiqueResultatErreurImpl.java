package ch.vd.uniregctb.editique.impl;

import ch.vd.uniregctb.editique.EditiqueResultatErreur;

public final class EditiqueResultatErreurImpl extends BaseEditiqueResultatImpl implements EditiqueResultatErreur {

	private final String error;
	private final long timestampReceived;

	public EditiqueResultatErreurImpl(String idDocument, String error, long timestampReceived) {
		super(idDocument);
		this.error = error;
		this.timestampReceived = timestampReceived;
	}

	@Override
	public String getError() {
		return error;
	}

	@Override
	public long getTimestampReceived() {
		return timestampReceived;
	}

	@Override
	public String getToStringComplement() {
		return String.format("error='%s'", error);
	}
}
