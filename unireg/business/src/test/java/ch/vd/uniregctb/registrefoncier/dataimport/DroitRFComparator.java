package ch.vd.uniregctb.registrefoncier.dataimport;

import java.util.Comparator;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.uniregctb.registrefoncier.DroitRF;

/**
 * Trie par masterIdRF, par ayant-droit id, par immeuble id et par date pour avoir qqch de stable.
  */
public class DroitRFComparator implements Comparator<DroitRF> {

	@Override
	public int compare(DroitRF o1, DroitRF o2) {
		final int c1 = compareString(o1.getMasterIdRF(), o2.getMasterIdRF());
		if (c1 != 0) {
			return c1;
		}
		final int c2 = compareString(o1.getAyantDroit().getIdRF(), o2.getAyantDroit().getIdRF());
		if (c2 != 0) {
			return c2;
		}
		final int c3 = compareString(o1.getImmeuble().getIdRF(), o2.getImmeuble().getIdRF());
		if (c3 != 0) {
			return c3;
		}
		return DateRangeComparator.compareRanges(o1, o2);
	}

	private static int compareString(@Nullable String idRF1, @Nullable String idRF2) {
		return MutationComparator.compareString(idRF1, idRF2);
	}
}
