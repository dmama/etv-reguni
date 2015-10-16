package ch.vd.uniregctb.rt.view;

import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;

public class SourcierListView extends TiersCriteriaView {

	private static final long serialVersionUID = 1L;

	private TiersGeneralView debiteur;

	private String provenance;

	public TiersGeneralView getDebiteur() {
		return debiteur;
	}

	public void setDebiteur(TiersGeneralView debiteur) {
		this.debiteur = debiteur;
	}

	public String getProvenance() {
		return provenance;
	}

	public void setProvenance(String provenance) {
		this.provenance = provenance;
	}
}
