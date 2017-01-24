package ch.vd.uniregctb.foncier;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;

@Embeddable
public class DonneesLoiLogement {

	private RegDate dateOctroi;
	private RegDate dateEcheance;

	public DonneesLoiLogement() {
	}

	public DonneesLoiLogement(RegDate dateOctroi, RegDate dateEcheance) {
		this.dateOctroi = dateOctroi;
		this.dateEcheance = dateEcheance;
	}

	@Column(name = "DATE_OCTROI")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateOctroi() {
		return dateOctroi;
	}

	public void setDateOctroi(RegDate dateOctroi) {
		this.dateOctroi = dateOctroi;
	}

	@Column(name = "DATE_ECHEANCE")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateEcheance() {
		return dateEcheance;
	}

	public void setDateEcheance(RegDate dateEcheance) {
		this.dateEcheance = dateEcheance;
	}
}
