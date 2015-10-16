package ch.vd.unireg.xml.common.v1;

import ch.vd.unireg.xml.common.v1.Date;

/**
 * Représente une période temporelle délimitée par deux dates. Si la date de début est nulle, la période est supposée exister depuis l'aube des temps. Si la date de fin est nulle, la période est
 * supposée exister jusqu'à la fin des temps.
 */
public interface DateRange {

	/**
	 * @return la date de début de la période (comprise dans la période); ou <b>null</b> pour dire que la période existe depuis l'aube des temps.
	 */
	Date getDateFrom();

	/**
	 * @return la date de fin de la période (comprise dans la période); ou <b>null</b> pour dire que la période existe jusqu'à la fin des temps.
	 */
	Date getDateTo();
}
