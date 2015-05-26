package ch.vd.uniregctb.migration.pm.rcent.model.base;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

/**
 * Représente une information valide pour la période comprise de
 * la date de début à la date de fin.
 */
public abstract class RCEntRangedElement implements DateRange {
	final private RegDate beginDate;
	final private RegDate endDateDate;

	public RCEntRangedElement(RegDate beginDate, RegDate endDateDate) {
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
