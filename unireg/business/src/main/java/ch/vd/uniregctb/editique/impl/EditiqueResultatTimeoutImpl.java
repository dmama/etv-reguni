package ch.vd.uniregctb.editique.impl;

import ch.vd.uniregctb.editique.EditiqueResultatTimeout;

public class EditiqueResultatTimeoutImpl extends BaseEditiqueResultatImpl implements EditiqueResultatTimeout {

	public EditiqueResultatTimeoutImpl(String idDocument) {
		super(idDocument);
	}

	@Override
	protected String getToStringComplement() {
		return null;
	}
}
