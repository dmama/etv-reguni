package ch.vd.uniregctb.xml.party.v5;

import ch.vd.registre.base.date.DateRange;
import ch.vd.unireg.xml.party.person.v5.ResidencyPeriod;
import ch.vd.uniregctb.xml.DataHelper;

public class ResidencyPeriodBuilder {

	public static ResidencyPeriod newPeriod(DateRange range) {
		final ResidencyPeriod period = new ResidencyPeriod();
		period.setDateFrom(DataHelper.coreToXMLv2(range.getDateDebut()));
		period.setDateTo(DataHelper.coreToXMLv2(range.getDateFin()));
		return period;
	}
}
