package ch.vd.unireg.entreprise.complexe;

import ch.vd.registre.base.date.RegDate;

public class ScissionEntrepriseView {

	private long idEntrepriseScindee;
	private RegDate dateContratScission;

	public ScissionEntrepriseView() {
	}

	public ScissionEntrepriseView(long idEntrepriseScindee) {
		this.idEntrepriseScindee = idEntrepriseScindee;
	}

	public ScissionEntrepriseView(long idEntrepriseScindee, RegDate dateContratScission) {
		this.idEntrepriseScindee = idEntrepriseScindee;
		this.dateContratScission = dateContratScission;
	}

	public long getIdEntrepriseScindee() {
		return idEntrepriseScindee;
	}

	public void setIdEntrepriseScindee(long idEntrepriseScindee) {
		this.idEntrepriseScindee = idEntrepriseScindee;
	}

	public RegDate getDateContratScission() {
		return dateContratScission;
	}

	public void setDateContratScission(RegDate dateContratScission) {
		this.dateContratScission = dateContratScission;
	}
}
