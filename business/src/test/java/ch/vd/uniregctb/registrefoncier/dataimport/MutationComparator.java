package ch.vd.uniregctb.registrefoncier.dataimport;

import java.util.Comparator;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;

/**
 * Trie par type, par idRF et par id pour avoir qqch de stable.
  */
public class MutationComparator implements Comparator<EvenementRFMutation> {

	@Override
	public int compare(EvenementRFMutation o1, EvenementRFMutation o2) {
		final int c1 = o1.getTypeEntite().compareTo(o2.getTypeEntite());
		if (c1 != 0) {
			return c1;
		}
		final int c2 = compareString(o1.getIdRF(), o2.getIdRF());
		if (c2 != 0) {
			return c2;
		}
		return o1.getId().compareTo(o2.getId());
	}

	public static int compareString(@Nullable String idRF1, @Nullable String idRF2) {
		if (idRF1 == null && idRF2 == null) {
			return 0;
		}
		else if (idRF1 == null) {
			return -1;
		}
		else if (idRF2 == null) {
			return 1;
		}
		else {
			return idRF1.compareTo(idRF2);
		}
	}
}
