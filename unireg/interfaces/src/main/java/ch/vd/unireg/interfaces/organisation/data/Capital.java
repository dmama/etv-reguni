package ch.vd.unireg.interfaces.organisation.data;

import java.math.BigDecimal;

public class Capital {

    private TypeDeCapital typeOfCapital;
    private String currency;
    private BigDecimal capitalAmount;
    private BigDecimal cashedInAmount;
    private String division;

   public Capital() {
		super();
	}

	public Capital(final TypeDeCapital typeOfCapital, final String currency, final BigDecimal capitalAmount, final BigDecimal cashedInAmount, final String division) {
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

	public TypeDeCapital getTypeOfCapital() {
		return typeOfCapital;
	}

	protected void setDivision(String division) {
		this.division = division;
	}

	protected void setCashedInAmount(BigDecimal cashedInAmount) {
		this.cashedInAmount = cashedInAmount;
	}

	protected void setCapitalAmount(BigDecimal capitalAmount) {
		this.capitalAmount = capitalAmount;
	}

	protected void setCurrency(String currency) {
		this.currency = currency;
	}

	protected void setTypeOfCapital(TypeDeCapital typeOfCapital) {
		this.typeOfCapital = typeOfCapital;
	}
}
