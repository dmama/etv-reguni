package ch.vd.unireg.registrefoncier.allegement;

import ch.vd.unireg.foncier.ExonerationIFONC;

public class EditExonerationView extends AbstractEditExonerationView {

	private long idExoneration;

	public EditExonerationView() {
	}

	public EditExonerationView(ExonerationIFONC exoneration) {
		super(exoneration);
		this.idExoneration = exoneration.getId();
	}

	public long getIdExoneration() {
		return idExoneration;
	}

	public void setIdExoneration(long idExoneration) {
		this.idExoneration = idExoneration;
	}
}
