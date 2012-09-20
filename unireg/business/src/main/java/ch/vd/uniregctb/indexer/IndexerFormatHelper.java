package ch.vd.uniregctb.indexer;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

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
	 * Convertit une objet en chaine le format qui va bien pour l'indexer
	 *
	 * @param value un objet
	 * @return la représentation string de l'objet
	 */
	public static String objectToString(Object value) {

		if (value == null) {
			return StringUtils.EMPTY;
		}

		if (value instanceof Collection) {
			final Collection<?> coll = (Collection<?>) value;
			final StringBuilder sb = new StringBuilder();
			for (Object o : coll) {
				if (sb.length() > 0) {
					sb.append(' ');
				}
				sb.append(objectToString(o));
			}
			return sb.toString();
		}
		else if (value instanceof Date) {
			return DateHelper.dateToIndexString((Date) value);
		}
		else if (value instanceof Boolean) {
			return value.equals(Boolean.TRUE) ? Constants.OUI : Constants.NON;
		}
		else if (value instanceof RegDate) {
			return RegDateHelper.toIndexString((RegDate) value);
		}
		else if (value instanceof Calendar) {
			Date date = ((Calendar) (value)).getTime();
			return DateHelper.dateToIndexString(date);
		}
		return value.toString();
	}

}
