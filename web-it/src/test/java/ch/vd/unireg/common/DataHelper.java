package ch.vd.uniregctb.common;

import ch.vd.registre.base.date.DateConstants;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

public abstract class DataHelper {
	private DataHelper() {
	}

	/**
	 * @param date une date partielle à convertir en {@link RegDate}
	 * @return la date convertie
	 * @throws IllegalArgumentException en cas de souci à la conversion (date résultante invalide, mauvais type de "date partielle" dont le mois  est inconnu mais pas le jour, par exemple...)
	 */
	public static RegDate xmlToCore(ch.vd.unireg.xml.common.v2.PartialDate date) throws IllegalArgumentException {
		if (date == null) {
			return null;
		}
		final int year = date.getYear();
		final Integer month = date.getMonth();
		final Integer day = date.getDay();
		if (day == null && month == null) {
			return RegDateHelper.get(year, DateConstants.EXTENDED_VALIDITY_RANGE);
		}
		else if (day == null) {
			return RegDateHelper.get(year, month, DateConstants.EXTENDED_VALIDITY_RANGE);
		}
		else if (month == null) {
			throw new IllegalArgumentException("Date partielle avec jour connu mais pas le mois : " + date);
		}
		else {
			return RegDateHelper.get(year, month, day, DateConstants.EXTENDED_VALIDITY_RANGE);
		}
	}
}
