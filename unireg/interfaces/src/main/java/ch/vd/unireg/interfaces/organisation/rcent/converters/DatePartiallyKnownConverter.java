package ch.vd.unireg.interfaces.organisation.rcent.converters;

import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v1.DatePartiallyKnown;
import ch.vd.registre.base.date.RegDate;

public class DatePartiallyKnownConverter extends BaseConverter<DatePartiallyKnown, RegDate> {

	@Override
	protected RegDate convert(@NotNull DatePartiallyKnown birth) {
		RegDate date = birth.getYearMonthDay();
		if (date == null) {
			XMLGregorianCalendar dm = birth.getYearMonth();
			if (dm != null) {
				date = RegDate.get(dm.getYear(), dm.getMonth());
			} else {
				dm = birth.getYear();
				date = RegDate.get(dm.getYear());
			}
		}
		return date;
	}

}
