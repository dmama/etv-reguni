package ch.vd.uniregctb.migration.pm.rcent.model;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;

public class RCEntAbstractHistoryElement implements DateRange {
	private RegDate beginDate;
	private RegDate endDateDate;

	public RCEntAbstractHistoryElement(RegDate beginDate, RegDate endDateDate) {
		this.beginDate = beginDate;
		this.endDateDate = endDateDate;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return date.compareTo(beginDate) >= 0 && (endDateDate == null || date.compareTo(endDateDate) <= 0);
	}

	@Override
	public RegDate getDateDebut() {
		return null;
	}

	@Override
	public RegDate getDateFin() {
		return null;
	}
}
