package ch.vd.uniregctb.entreprise.complexe;

import ch.vd.registre.base.date.RegDate;

public class AnnulationDemenagementSiegeView {

	private long idEntreprise;
	private RegDate dateDebutSiegeActuel;

	public AnnulationDemenagementSiegeView() {
	}

	public AnnulationDemenagementSiegeView(long idEntreprise, RegDate dateDebutSiegeActuel) {
		this.idEntreprise = idEntreprise;
		this.dateDebutSiegeActuel = dateDebutSiegeActuel;
	}

	public long getIdEntreprise() {
		return idEntreprise;
	}

	public void setIdEntreprise(long idEntreprise) {
		this.idEntreprise = idEntreprise;
	}

	public RegDate getDateDebutSiegeActuel() {
		return dateDebutSiegeActuel;
	}

	public void setDateDebutSiegeActuel(RegDate dateDebutSiegeActuel) {
		this.dateDebutSiegeActuel = dateDebutSiegeActuel;
	}
}
