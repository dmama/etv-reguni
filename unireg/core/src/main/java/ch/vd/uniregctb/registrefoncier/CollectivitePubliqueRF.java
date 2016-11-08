package ch.vd.uniregctb.registrefoncier;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("CollectivitePublique")
public class CollectivitePubliqueRF extends TiersRF {

	private String raisonSociale;

	@Column(name = "RAISON_SOCIALE")
	public String getRaisonSociale() {
		return raisonSociale;
	}

	public void setRaisonSociale(String raisonSociale) {
		this.raisonSociale = raisonSociale;
	}

	@Override
	public void copyDataTo(AyantDroitRF right) {
		super.copyDataTo(right);
		final CollectivitePubliqueRF collRight = (CollectivitePubliqueRF) right;
		collRight.raisonSociale = this.raisonSociale;
	}
}
