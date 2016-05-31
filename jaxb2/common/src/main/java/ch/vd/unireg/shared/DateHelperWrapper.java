package ch.vd.unireg.shared;

import java.util.Date;

import ch.vd.registre.base.date.DateHelper;

public class DateHelperWrapper {

	public static Date isoTimestampToDate(String isoTimeStamp) {
		try {
			return DateHelper.isoTimestampToDate(isoTimeStamp);
		}
		catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static String dateToIsoTimestamp(Date date) {
		try {
			return DateHelper.dateToIsoTimestamp(date);
		}
		catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}
}
