package ch.vd.uniregctb.editique.impl;

import ch.vd.uniregctb.editique.EditiqueResultatReroutageInbox;

/**
 * Interface (marquage seulement) d'un résultat qui indique
 * que la demande d'impression a été re-routée vers le mécanisme asynchrone
 * dont le résultat sera livré dans la boîte de réception de l'appelant
 */
public final class EditiqueResultatReroutageInboxImpl extends BaseEditiqueResultatImpl implements EditiqueResultatReroutageInbox {

	public EditiqueResultatReroutageInboxImpl(String idDocument) {
		super(idDocument);
	}

	@Override
	protected String getToStringComplement() {
		return null;
	}
}
