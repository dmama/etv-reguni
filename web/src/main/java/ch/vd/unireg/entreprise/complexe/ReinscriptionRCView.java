package ch.vd.unireg.entreprise.complexe;

import ch.vd.registre.base.date.RegDate;

public class ReinscriptionRCView {

	private long idEntreprise;
	private RegDate dateRadiationRC;
	private String remarque;

	public ReinscriptionRCView() {
	}

	public ReinscriptionRCView(long idEntreprise, RegDate dateRadiationRC) {
		this.idEntreprise = idEntreprise;
		this.dateRadiationRC = dateRadiationRC;
	}

	public long getIdEntreprise() {
		return idEntreprise;
	}

	public void setIdEntreprise(long idEntreprise) {
		this.idEntreprise = idEntreprise;
	}

	public RegDate getDateRadiationRC() {
		return dateRadiationRC;
	}

	public void setDateRadiationRC(RegDate dateRadiationRC) {
		this.dateRadiationRC = dateRadiationRC;
	}

	public String getRemarque() {
		return remarque;
	}

	public void setRemarque(String remarque) {
		this.remarque = remarque;
	}
}
