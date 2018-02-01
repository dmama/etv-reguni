package ch.vd.unireg.foncier;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.math.BigDecimal;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;

@Embeddable
public class DonneesLoiLogement {

	private Boolean controleOfficeLogement;
	private RegDate dateOctroi;
	private RegDate dateEcheance;
	private BigDecimal pourcentageCaractereSocial;

	public DonneesLoiLogement() {
	}

	public DonneesLoiLogement(Boolean controleOfficeLogement, RegDate dateOctroi, RegDate dateEcheance, BigDecimal pourcentageCaractereSocial) {
		this.controleOfficeLogement = controleOfficeLogement;
		this.dateOctroi = dateOctroi;
		this.dateEcheance = dateEcheance;
		this.pourcentageCaractereSocial = pourcentageCaractereSocial;
	}

	public DonneesLoiLogement(DonneesLoiLogement src) {
		this(src.controleOfficeLogement, src.dateOctroi, src.dateEcheance, src.pourcentageCaractereSocial);
	}

	@Column(name = "DATE_OCTROI")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateOctroi() {
		return dateOctroi;
	}

	public void setDateOctroi(RegDate dateOctroi) {
		this.dateOctroi = dateOctroi;
	}

	@Column(name = "DATE_ECHEANCE")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
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

	@Column(name = "CTRL_OFFICE_LOGEMENT")
	public Boolean getControleOfficeLogement() {
		return controleOfficeLogement;
	}

	public void setControleOfficeLogement(Boolean controleOfficeLogement) {
		this.controleOfficeLogement = controleOfficeLogement;
	}
}
