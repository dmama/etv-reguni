package ch.vd.uniregctb.contribuableAssocie.view;

import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;

public class ContribuableAssocieListView  extends TiersCriteriaView {

	private static final long serialVersionUID = -2659171967019241854L;

	private TiersGeneralView debiteur;

	public TiersGeneralView getDebiteur() {
		return debiteur;
	}

	public void setDebiteur(TiersGeneralView debiteur) {
		this.debiteur = debiteur;
	}

}
