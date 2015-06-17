package ch.vd.unireg.interfaces.organisation.data;

import java.math.BigDecimal;

public class Capital {

    private TypeOfCapital typeOfCapital;
    private String currency;
    private BigDecimal capitalAmount;
    private BigDecimal cashedInAmount;
    private String division;

   public Capital() {
		super();
	}

	public Capital(final TypeOfCapital typeOfCapital, final String currency, final BigDecimal capitalAmount, final BigDecimal cashedInAmount, final String division) {
		this.typeOfCapital = typeOfCapital;
		this.currency = currency;
		this.capitalAmount = capitalAmount;
		this.cashedInAmount = cashedInAmount;
		this.division = division;
	}

	public BigDecimal getCapitalAmount() {
		return capitalAmount;
	}

	public BigDecimal getCashedInAmount() {
		return cashedInAmount;
	}

	public String getCurrency() {
		return currency;
	}

	public String getDivision() {
		return division;
	}

	public TypeOfCapital getTypeOfCapital() {
		return typeOfCapital;
	}
}
