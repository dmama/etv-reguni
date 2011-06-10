package ch.vd.uniregctb.tiers.view;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import ch.vd.uniregctb.common.GentilComparator;

/**
 * Compare les vues sur les fors fiscaux compte tenu de l'algorithme de tri suivant:
 * <ul>
 * <li>Annulé / pas annulé (annulé toujours après)</li>
 * <li>Fors principaux d'abord, puis secondaires, puis les autres</li>
 * <li>Date d'ouverture décroissante du for</li>
 * <li>Date d'événement décroissante du for</li>
 * <li>Genre d'impôt</li>
 * <li>Motif de rattachement</li>
 * </ul>
 */
public class ForFiscalViewComparator implements Comparator<ForFiscalView> {

	private static final List<String> ordreNatureFor = Arrays.asList("ForFiscalPrincipal", "ForFiscalSecondaire");

	private static final Comparator<String> comparatorNatureFor = new GentilComparator<String>(ordreNatureFor);

	private static <T extends Comparable<T>> int compareNullable(T o1, T o2, boolean nullAtEnd) {
		if (o1 == o2) {
			return 0;
		}
		else if (o1 == null) {
			return nullAtEnd ? -1 : 1;
		}
		else if (o2 == null) {
			return nullAtEnd ? 1 : -1;
		}
		else {
			return o1.compareTo(o2);
		}
	}

	@Override
	public int compare(ForFiscalView o1, ForFiscalView o2) {
		int compare = Boolean.valueOf(o1.isAnnule()).compareTo(o2.isAnnule());
		if (compare == 0) {
			compare = comparatorNatureFor.compare(o1.getNatureForFiscal(), o2.getNatureForFiscal());
		}
		if (compare == 0) {
			compare = - compareNullable(o1.getDateOuverture(), o2.getDateOuverture(), false);
		}
		if (compare == 0) {
			compare = - compareNullable(o1.getDateEvenement(), o2.getDateEvenement(), true);
		}
		if (compare == 0) {
			compare = compareNullable(o1.getGenreImpot(), o2.getGenreImpot(), true);
		}
		if (compare == 0) {
			compare = compareNullable(o1.getMotifRattachement(), o2.getMotifRattachement(), true);
		}
		return compare;
	}
}