package ch.vd.unireg.interfaces.organisation.data;

import java.util.Comparator;

import ch.vd.registre.base.date.RegDate;

/**
 * @author Raphaël Marmier, 2015-08-19
 */
public class DateRangedComparator <T> implements Comparator<DateRanged<T>> {

	public DateRangedComparator() {}

	// TODO: Write test
	public int compare(DateRanged<T> o1, DateRanged<T> o2) {
		if (o1 == null && o2 == null) {
			return 0;
		}
		else if (o1 == null) {
			return -1;
		}
		else if (o2 == null) {
			return 1;
		}
		else {
			RegDate d1 = o1.getDateDebut();
			RegDate d2 = o2.getDateDebut();
			if ((d1 != null || d2 != null) && (d1 == null || d2 == null || !d1.equals(d2))) {
				if (d1 == null) {
					return -1;
				}

				if (d2 == null) {
					return 1;
				}
			}
			else {
				d1 = o1.getDateFin();
				d2 = o2.getDateFin();
				if (d1 == null && d2 == null) {
					return 0;
				}

				if (d1 == null) {
					return 1;
				}

				if (d2 == null) {
					return -1;
				}
			}

			return d1.compareTo(d2);
		}
	}

}
