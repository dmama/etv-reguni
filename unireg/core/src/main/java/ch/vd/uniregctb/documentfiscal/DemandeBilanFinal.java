package ch.vd.uniregctb.documentfiscal;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;

@Entity
@DiscriminatorValue(value = "DemandeBilanFinal")
public class DemandeBilanFinal extends AutreDocumentFiscal {

	private int periodeFiscale;
	private RegDate dateRequisitionRadiation;

	@Column(name = "DBF_PERIODE_FISCALE")
	public int getPeriodeFiscale() {
		return periodeFiscale;
	}

	public void setPeriodeFiscale(int periodeFiscale) {
		this.periodeFiscale = periodeFiscale;
	}

	@Column(name = "DBF_DATE_REQ_RADIATION")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateRequisitionRadiation() {
		return dateRequisitionRadiation;
	}

	public void setDateRequisitionRadiation(RegDate dateRequisitionRadiation) {
		this.dateRequisitionRadiation = dateRequisitionRadiation;
	}
}
