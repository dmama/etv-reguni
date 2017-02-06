package ch.vd.uniregctb.foncier;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.math.BigDecimal;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;

@Embeddable
public class DonneesLoiLogement {

	private RegDate dateOctroi;
	private RegDate dateEcheance;
	private BigDecimal pourcentageCaractereSocial;

	public DonneesLoiLogement() {
	}

	public DonneesLoiLogement(RegDate dateOctroi, RegDate dateEcheance, BigDecimal pourcentageCaractereSocial) {
		this.dateOctroi = dateOctroi;
		this.dateEcheance = dateEcheance;
		this.pourcentageCaractereSocial = pourcentageCaractereSocial;
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

	@Column(name = "CARAC_SOCIAL_POURCENT", precision = 5, scale = 2)
	public BigDecimal getPourcentageCaractereSocial() {
		return pourcentageCaractereSocial;
	}

	public void setPourcentageCaractereSocial(BigDecimal pourcentageCaractereSocial) {
		this.pourcentageCaractereSocial = pourcentageCaractereSocial;
	}
}
