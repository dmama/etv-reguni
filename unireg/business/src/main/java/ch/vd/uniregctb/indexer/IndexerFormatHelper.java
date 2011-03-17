package ch.vd.uniregctb.indexer;

import java.util.Calendar;
import java.util.Date;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.Constants;

public class IndexerFormatHelper {

	public static String formatNumeroAVS(String noAVS) {

		if (noAVS != null) {
			String noAVS2 = noAVS.trim();
			noAVS2 = noAVS2.replaceAll("\\.", "");
			noAVS2 = noAVS2.replaceAll(" ", "");
			noAVS2 = noAVS2.replaceAll("-", "");
			return noAVS2;
		}
		return null;
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
