package ch.vd.uniregctb.registrefoncier.allegement;

import ch.vd.uniregctb.foncier.DemandeDegrevementICI;

public class EditDemandeDegrevementView extends AbstractEditDemandeDegrevementView {

	private long idDemandeDegrevement;

	public EditDemandeDegrevementView() {
	}

	public EditDemandeDegrevementView(DemandeDegrevementICI demande) {
		super(demande);
		this.idDemandeDegrevement = demande.getId();
	}

	public long getIdDemandeDegrevement() {
		return idDemandeDegrevement;
	}

	public void setIdDemandeDegrevement(long idDemandeDegrevement) {
		this.idDemandeDegrevement = idDemandeDegrevement;
	}
}
