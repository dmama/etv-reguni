package ch.vd.unireg.interfaces.entreprise.data.builder;

import java.math.BigDecimal;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.entreprise.data.Capital;
import ch.vd.unireg.interfaces.entreprise.data.TypeDeCapital;

public class CapitalBuilder {

	private RegDate dateDebut;
	private RegDate dateFin;
	private BigDecimal capitalAmount;
	private TypeDeCapital typeOfCapital;
	private String currency;
	private String division;

	public CapitalBuilder() {
	}

	public CapitalBuilder withCapitalAmount(BigDecimal capitalAmount) {
		this.capitalAmount = capitalAmount;
		return this;
	}

	public CapitalBuilder withTypeOfCapital(TypeDeCapital typeOfCapital) {
		this.typeOfCapital = typeOfCapital;
		return this;
	}

	public CapitalBuilder withCurrency(String currency) {
		this.currency = currency;
		return this;
	}

	public CapitalBuilder withDivision(String division) {
		this.division = division;
		return this;
	}

	public CapitalBuilder withDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
		return this;
	}

	public CapitalBuilder withDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
		return this;
	}

	public Capital build() {
		return new Capital(dateDebut, dateFin, typeOfCapital, currency, capitalAmount, division);
	}
}
