package ch.vd.unireg.interfaces.infra.fidor;

import ch.vd.fidor.xml.common.v1.Date;
import ch.vd.registre.base.date.RegDate;

/**
 * Helper class pour tout ce qui concerne les XSD fidor
 */
public abstract class XmlUtils {

	public static RegDate toRegDate(Date date) {
		if (date == null) {
			return null;
		}

		final int year = date.getYear();
		final int month = date.getMonth();
		final int day = date.getDay();
		return RegDate.get(year, month, day);
	}

}
