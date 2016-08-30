package ch.vd.uniregctb.entreprise.complexe;

import ch.vd.registre.base.date.RegDate;

public class FusionEntreprisesView {

	private long idEntrepriseAbsorbante;
	private RegDate dateContratFusion;
	private RegDate dateBilanFusion;

	public FusionEntreprisesView() {
	}

	public FusionEntreprisesView(long idEntrepriseAbsorbante) {
		this.idEntrepriseAbsorbante = idEntrepriseAbsorbante;
	}

	public FusionEntreprisesView(long idEntrepriseAbsorbante, RegDate dateContratFusion, RegDate dateBilanFusion) {
		this.idEntrepriseAbsorbante = idEntrepriseAbsorbante;
		this.dateContratFusion = dateContratFusion;
		this.dateBilanFusion = dateBilanFusion;
	}

	public long getIdEntrepriseAbsorbante() {
		return idEntrepriseAbsorbante;
	}

	public void setIdEntrepriseAbsorbante(long idEntrepriseAbsorbante) {
		this.idEntrepriseAbsorbante = idEntrepriseAbsorbante;
	}

	public RegDate getDateContratFusion() {
		return dateContratFusion;
	}

	public void setDateContratFusion(RegDate dateContratFusion) {
		this.dateContratFusion = dateContratFusion;
	}

	public RegDate getDateBilanFusion() {
		return dateBilanFusion;
	}

	public void setDateBilanFusion(RegDate dateBilanFusion) {
		this.dateBilanFusion = dateBilanFusion;
	}
}
