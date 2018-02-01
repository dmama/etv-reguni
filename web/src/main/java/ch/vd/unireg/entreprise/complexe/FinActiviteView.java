package ch.vd.unireg.entreprise.complexe;

import ch.vd.registre.base.date.RegDate;

public class FinActiviteView {

	private long idEntreprise;
	private RegDate dateFinActivite;
	private String remarque;

	public FinActiviteView() {
	}

	public FinActiviteView(long idEntreprise) {
		this.idEntreprise = idEntreprise;
	}

	public long getIdEntreprise() {
		return idEntreprise;
	}

	public void setIdEntreprise(long idEntreprise) {
		this.idEntreprise = idEntreprise;
	}

	public RegDate getDateFinActivite() {
		return dateFinActivite;
	}

	public void setDateFinActivite(RegDate dateFinActivite) {
		this.dateFinActivite = dateFinActivite;
	}

	public String getRemarque() {
		return remarque;
	}

	public void setRemarque(String remarque) {
		this.remarque = remarque;
	}
}
