package ch.vd.unireg.identification.contribuable.view;

import ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuable;

public class NonIdentificationMessageEditView {

	private IdentificationContribuable.ErreurMessage erreurMessage;

	public IdentificationContribuable.ErreurMessage getErreurMessage() {
		return erreurMessage;
	}

	public void setErreurMessage(IdentificationContribuable.ErreurMessage erreurMessage) {
		this.erreurMessage = erreurMessage;
	}
}
