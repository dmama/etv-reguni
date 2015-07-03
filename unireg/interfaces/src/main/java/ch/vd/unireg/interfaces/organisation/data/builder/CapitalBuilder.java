package ch.vd.unireg.interfaces.organisation.data.builder;

import java.math.BigDecimal;

import ch.vd.unireg.interfaces.organisation.data.Capital;
import ch.vd.unireg.interfaces.organisation.data.TypeDeCapital;

public class CapitalBuilder {

	private BigDecimal capitalAmount;
	private TypeDeCapital typeOfCapital;
	private String currency;
	private BigDecimal cashedInAmount;
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

	public CapitalBuilder withCashedInAmount(BigDecimal cashedInAmount) {
		this.cashedInAmount = cashedInAmount;
		return this;
	}

	public CapitalBuilder withDivision(String division) {
		this.division = division;
		return this;
	}

	public Capital build() {
		return new Capital(typeOfCapital, currency, capitalAmount, cashedInAmount, division);
	}
}
