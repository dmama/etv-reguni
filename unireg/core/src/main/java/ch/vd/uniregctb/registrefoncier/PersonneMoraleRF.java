package ch.vd.uniregctb.registrefoncier;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import ch.vd.uniregctb.common.LengthConstants;

@Entity
@DiscriminatorValue("PersonneMorale")
public class PersonneMoraleRF extends TiersRF {

	/**
	 * La raison sociale de la personne morale telle que connue dans le RF
	 */
	private String raisonSociale;

	/**
	 * Le numéro RC/FOSC de la personne morale, s'il existe et est connu dans le RF (forme sans tiret ni point, si possible...)
	 */
	private String numeroRC;

	@Column(name = "RAISON_SOCIALE", length = LengthConstants.RF_TIERS_RAISON_SOCIALE)
	public String getRaisonSociale() {
		return raisonSociale;
	}

	public void setRaisonSociale(String raisonSociale) {
		this.raisonSociale = raisonSociale;
	}

	@Column(name = "NUMERO_RC", length = LengthConstants.RF_PM_NUMRC)
	public String getNumeroRC() {
		return numeroRC;
	}

	public void setNumeroRC(String numeroRC) {
		this.numeroRC = numeroRC;
	}

	@Override
	public void copyDataTo(AyantDroitRF right) {
		super.copyDataTo(right);
		final PersonneMoraleRF pmRight = (PersonneMoraleRF) right;
		pmRight.raisonSociale = this.raisonSociale;
		pmRight.numeroRC = this.numeroRC;
	}

}
