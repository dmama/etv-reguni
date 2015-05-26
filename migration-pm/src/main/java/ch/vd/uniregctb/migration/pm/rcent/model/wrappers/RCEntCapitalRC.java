package ch.vd.uniregctb.migration.pm.rcent.model.wrappers;

import java.math.BigDecimal;

import ch.vd.evd0022.v1.Capital;
import ch.vd.evd0022.v1.TypeOfCapital;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.rcent.model.base.RCEntRangedWrapper;

public class RCEntCapitalRC extends RCEntRangedWrapper<Capital> {
	public RCEntCapitalRC(RegDate beginDate, RegDate endDateDate, Capital element) {
		super(beginDate, endDateDate, element);
	}

	public BigDecimal getCapitalAmount() {
		return getElement().getCapitalAmount();
	}

	public String getCurrency() {
		return getElement().getCurrency();
	}

	public BigDecimal getCashedInAmount() {
		return getElement().getCashedInAmount();
	}

	public String getDivision() {
		return getElement().getDivision();
	}

	public TypeOfCapital getTypeOfCapital() {
		return getElement().getTypeOfCapital();
	}
}
