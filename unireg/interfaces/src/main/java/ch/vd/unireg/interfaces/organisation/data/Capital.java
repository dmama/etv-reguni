package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;
import java.math.BigDecimal;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

public class Capital implements Serializable, DateRange, DateRangeLimitable<Capital> {

	private static final long serialVersionUID = 5621443460846552394L;

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final TypeDeCapital typeOfCapital;
    private final String currency;
    private final BigDecimal capitalAmount;
	private final String division;

	public Capital(RegDate dateDebut, @Nullable RegDate dateFin, TypeDeCapital typeOfCapital, String currency, BigDecimal capitalAmount, String division) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.typeOfCapital = typeOfCapital;
		this.currency = currency;
		this.capitalAmount = capitalAmount;
		this.division = division;
	}

	public Capital limitTo(@Nullable RegDate dateDebut, @Nullable RegDate dateFin) {
		return new Capital(dateDebut == null ? this.dateDebut : dateDebut,
		                   dateFin == null ? this.dateFin : dateFin,
		                   this.typeOfCapital,
		                   this.currency,
		                   this.capitalAmount,
		                   this.division);
	}

	public BigDecimal getCapitalAmount() {
		return capitalAmount;
	}

	public String getCurrency() {
		return currency;
	}

	public TypeDeCapital getTypeOfCapital() {
		return typeOfCapital;
	}

	public String getDivision() {
		return division;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}
}
