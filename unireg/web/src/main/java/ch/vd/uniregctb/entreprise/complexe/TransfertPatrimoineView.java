package ch.vd.uniregctb.entreprise.complexe;

import ch.vd.registre.base.date.RegDate;

public class TransfertPatrimoineView {

	private long idEntrepriseEmettrice;
	private RegDate dateTransfert;

	public TransfertPatrimoineView() {
	}

	public TransfertPatrimoineView(long idEntrepriseEmettrice) {
		this.idEntrepriseEmettrice = idEntrepriseEmettrice;
	}

	public TransfertPatrimoineView(long idEntrepriseEmettrice, RegDate dateTransfert) {
		this.idEntrepriseEmettrice = idEntrepriseEmettrice;
		this.dateTransfert = dateTransfert;
	}

	public long getIdEntrepriseEmettrice() {
		return idEntrepriseEmettrice;
	}

	public void setIdEntrepriseEmettrice(long idEntrepriseEmettrice) {
		this.idEntrepriseEmettrice = idEntrepriseEmettrice;
	}

	public RegDate getDateTransfert() {
		return dateTransfert;
	}

	public void setDateTransfert(RegDate dateTransfert) {
		this.dateTransfert = dateTransfert;
	}
}
