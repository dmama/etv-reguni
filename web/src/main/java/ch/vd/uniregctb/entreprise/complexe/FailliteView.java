package ch.vd.uniregctb.entreprise.complexe;

import ch.vd.registre.base.date.RegDate;

public class FailliteView {

	private long idEntreprise;
	private RegDate datePrononceFaillite;
	private String remarque;

	public FailliteView() {
	}

	public FailliteView(long idEntreprise) {
		this.idEntreprise = idEntreprise;
	}

	public long getIdEntreprise() {
		return idEntreprise;
	}

	public void setIdEntreprise(long idEntreprise) {
		this.idEntreprise = idEntreprise;
	}

	public RegDate getDatePrononceFaillite() {
		return datePrononceFaillite;
	}

	public void setDatePrononceFaillite(RegDate datePrononceFaillite) {
		this.datePrononceFaillite = datePrononceFaillite;
	}

	public String getRemarque() {
		return remarque;
	}

	public void setRemarque(String remarque) {
		this.remarque = remarque;
	}
}
