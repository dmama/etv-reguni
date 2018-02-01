package ch.vd.unireg.documentfiscal;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;

@Entity
@DiscriminatorValue(value = "DemandeBilanFinal")
public class DemandeBilanFinal extends AutreDocumentFiscal {

	private Integer periodeFiscale;
	private RegDate dateRequisitionRadiation;

	@Override
	@Column(name = "DBF_PERIODE_FISCALE")
	public Integer getPeriodeFiscale() {
		return periodeFiscale;
	}

	public void setPeriodeFiscale(Integer periodeFiscale) {
		this.periodeFiscale = periodeFiscale;
	}

	@Column(name = "DBF_DATE_REQ_RADIATION")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateRequisitionRadiation() {
		return dateRequisitionRadiation;
	}

	public void setDateRequisitionRadiation(RegDate dateRequisitionRadiation) {
		this.dateRequisitionRadiation = dateRequisitionRadiation;
	}
}
