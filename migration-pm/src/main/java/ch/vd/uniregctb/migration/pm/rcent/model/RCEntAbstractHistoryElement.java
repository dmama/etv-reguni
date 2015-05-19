package ch.vd.uniregctb.migration.pm.rcent.model;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

public class RCEntAbstractHistoryElement implements DateRange {
	final private RegDate beginDate;
	final private RegDate endDateDate;

	public RCEntAbstractHistoryElement(RegDate beginDate, RegDate endDateDate) {
		this.beginDate = beginDate;
		this.endDateDate = endDateDate;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, this.beginDate, this.endDateDate, NullDateBehavior.LATEST);
	}

	@Override
	public RegDate getDateDebut() {
		return beginDate;
	}

	@Override
	public RegDate getDateFin() {
		return endDateDate;
	}
}
