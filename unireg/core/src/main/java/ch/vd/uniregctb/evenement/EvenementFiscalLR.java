package ch.vd.uniregctb.evenement;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;

@Entity
@DiscriminatorValue("EvenementFiscalLR")
public class EvenementFiscalLR extends EvenementFiscal{

	/**
	 *
	 */
	private static final long serialVersionUID = 3272714271175858488L;
	private RegDate dateDebutPeriode;
	private RegDate dateFinPeriode;

	@Column(name = "DATE_DEBUT_PERIODE")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDebutPeriode() {
		return dateDebutPeriode;
	}

	public void setDateDebutPeriode(RegDate dateDebutPeriode) {
		this.dateDebutPeriode = dateDebutPeriode;
	}

	@Column(name = "DATE_FIN_PERIODE")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateFinPeriode() {
		return dateFinPeriode;
	}

	public void setDateFinPeriode(RegDate dateFinPeriode) {
		this.dateFinPeriode = dateFinPeriode;
	}


}
