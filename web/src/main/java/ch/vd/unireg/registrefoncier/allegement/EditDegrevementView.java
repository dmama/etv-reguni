package ch.vd.unireg.registrefoncier.allegement;

import ch.vd.unireg.foncier.DegrevementICI;

public class EditDegrevementView extends AbstractEditDegrevementView {

	private long idDegrevement;

	public EditDegrevementView() {
	}

	public EditDegrevementView(DegrevementICI degrevement) {
		super(degrevement);
		this.idDegrevement = degrevement.getId();
	}

	public long getIdDegrevement() {
		return idDegrevement;
	}

	public void setIdDegrevement(long idDegrevement) {
		this.idDegrevement = idDegrevement;
	}
}
