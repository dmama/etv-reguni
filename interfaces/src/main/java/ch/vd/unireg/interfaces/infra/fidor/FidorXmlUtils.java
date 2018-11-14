package ch.vd.unireg.interfaces.infra.fidor;

import org.jetbrains.annotations.NotNull;

import ch.vd.fidor.xml.common.v1.Date;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.xml.common.v2.XmlDateRangeFr;

/**
 * Helper class pour tout ce qui concerne les XSD fidor
 */
public abstract class FidorXmlUtils {

	public static RegDate toRegDate(Date date) {
		if (date == null) {
			return null;
		}

		final int year = date.getYear();
		final int month = date.getMonth();
		final int day = date.getDay();
		return RegDate.get(year, month, day);
	}

	/**
	 * Vérifie la validité du range pour une date donnée.
	 * <p>
	 * Un objet est dit valide pour une date <b>non-nulle</b> si:
	 *
	 * <pre>
	 *  date = [dateDebut, dateFin]
	 * </pre>
	 * <p>
	 * ... et pour une date <b>nulle</b> si:
	 *
	 * <pre>
	 * dateFin == null
	 * </pre>
	 *
	 * @param date la date de référence, ou <b>null</b> pour vérifier si l'objet est actif
	 * @return vrai si le range est valide, faux autrement
	 */
	public static boolean isValid(@NotNull XmlDateRangeFr range, RegDate date) {
		return RegDateHelper.isBetween(date, ch.vd.registre.base.xml.XmlUtils.cal2regdate(range.getDateDebut()), ch.vd.registre.base.xml.XmlUtils.cal2regdate(range.getDateFin()), NullDateBehavior.LATEST);
	}
}
