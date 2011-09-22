package ch.vd.uniregctb.contribuableAssocie.view;

import ch.vd.uniregctb.general.view.TiersGeneralView;

public class ContribuableAssocieEditView {

	private TiersGeneralView contribuable;
	private TiersGeneralView debiteur;
	private boolean isAllowed;

	public TiersGeneralView getContribuable() {
		return contribuable;
	}
	public void setContribuable(TiersGeneralView contribuable) {
		this.contribuable = contribuable;
	}
	public TiersGeneralView getDebiteur() {
		return debiteur;
	}
	public void setDebiteur(TiersGeneralView debiteur) {
		this.debiteur = debiteur;
	}
	public boolean isAllowed() {
		return isAllowed;
	}
	public void setAllowed(boolean isAllowed) {
		this.isAllowed = isAllowed;
	}

}
