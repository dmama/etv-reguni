package ch.vd.uniregctb.documentfiscal;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;

@Entity
@DiscriminatorValue(value = "AutorisationRadiationRC")
public class AutorisationRadiationRC extends AutreDocumentFiscal {

	/**
	 * La date de la demande de radiation faite par le RC
	 */
	private RegDate dateDemande;

	@Column(name = "AR_DATE_DEMANDE")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDemande() {
		return dateDemande;
	}

	public void setDateDemande(RegDate dateDemande) {
		this.dateDemande = dateDemande;
	}
}
