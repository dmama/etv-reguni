package ch.vd.uniregctb.tiers.timeline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;

public abstract class TimelineHelper {

	/**
	 * A partir d'une liste de ranges (qui peuvent se recouvrir), détermine la liste triées par ordre croissant des dates de début et de fin des ranges. Chaque date n'apparaît qu'une fois dans la liste.
	 * <p/>
	 * <b>Note:</b> Si au moins un des ranges possède une date de début nulle, la première date de la liste est nulle. De même, si au moins un des ranges possède une date de fin nulle, la dernière date
	 * de la liste est nulle.
	 * <p/>
	 * <pre>
	 * Ranges d'entrées:
	 * +----------------------+                           +----------------+
	 * ¦x                    y¦                           ¦u              v¦
	 * +----------------------+                           +----------------+
	 *                      +----------------------+              +----------------+
	 *                      ¦y                    z¦              ¦r              s¦
	 *                      +----------------------+              +----------------+
	 * .
	 * Dates de sortie: x, y, y+1, z+1, u, r, v+1, s+1
	 * </pre>
	 */
	public static List<RegDate> extractBoundaries(Collection<? extends DateRange> ranges) {

		boolean dateDebutNull = false;
		boolean dateFinNull = false;

		// Détermination de l'ensemble des dates
		final SortedSet<RegDate> dates = new TreeSet<RegDate>();
		for (DateRange r : ranges) {
			final RegDate dateDebut = r.getDateDebut();
			if (dateDebut == null) {
				dateDebutNull = true;
			}
			else {
				dates.add(dateDebut);
			}
			RegDate dateFin = r.getDateFin();
			if (dateFin == null) {
				dateFinNull = true;
			}
			else {
				dates.add(dateFin.getOneDayAfter());
			}
		}

		// Transforme l'ensemble en liste + traitement des valeurs nulles
		List<RegDate> list = new ArrayList<RegDate>(dates);
		if (dateDebutNull) {
			list.add(0, null);
		}
		if (dateFinNull) {
			list.add(null);
		}

		return list;
	}
}
