package ch.vd.uniregctb.indexer;

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.Constants;

public class IndexerFormatHelper {

	private static Pattern PATTERN= Pattern.compile("[-. \t]");

	public static String formatNumeroAVS(String noAVS) {
		if (noAVS != null) {
			noAVS = PATTERN.matcher(noAVS).replaceAll("");
		}
		return noAVS;
	}

	/**
	 * Convertit une date en chaine dans le format DATE_FORMAT_INDEX
	 *
	 * @param value
	 * @return une chaine
	 */
	public static String objectToString(Object value) {

		if (value != null) {
			if (value instanceof Date) {
				return DateHelper.dateToIndexString((Date)value);
			}
			if (value instanceof Boolean) {
				return value.equals(Boolean.TRUE) ? Constants.OUI : Constants.NON;
			}
			else if (value instanceof RegDate) {
				return RegDateHelper.toIndexString((RegDate)value);
			}
			else if (value instanceof Calendar) {
				Date date = ((Calendar) (value)).getTime();
				return DateHelper.dateToIndexString(date);
			}
			return value.toString();
		}
		return "";
	}

}
