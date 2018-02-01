package ch.vd.unireg.param.view;

import ch.vd.registre.base.date.RegDate;

public class ParametrePeriodeFiscaleSNCEditView {

	private Long idPeriodeFiscale;
	private Integer anneePeriodeFiscale;

	private RegDate rappelReglementaire;
	private RegDate rappelEffectif;

	public Long getIdPeriodeFiscale() {
		return idPeriodeFiscale;
	}

	public void setIdPeriodeFiscale(Long idPeriodeFiscale) {
		this.idPeriodeFiscale = idPeriodeFiscale;
	}

	public Integer getAnneePeriodeFiscale() {
		return anneePeriodeFiscale;
	}

	public void setAnneePeriodeFiscale(Integer anneePeriodeFiscale) {
		this.anneePeriodeFiscale = anneePeriodeFiscale;
	}

	public RegDate getRappelReglementaire() {
		return rappelReglementaire;
	}

	public void setRappelReglementaire(RegDate rappelReglementaire) {
		this.rappelReglementaire = rappelReglementaire;
	}

	public RegDate getRappelEffectif() {
		return rappelEffectif;
	}

	public void setRappelEffectif(RegDate rappelEffectif) {
		this.rappelEffectif = rappelEffectif;
	}
}
