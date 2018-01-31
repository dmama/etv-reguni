package ch.vd.uniregctb.registrefoncier;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import ch.vd.registre.base.date.DateRangeComparator;

/**
 * Comparateur de droits RF qui utilise les critères suivants pour les ordonner les valeurs :
 * <ul>
 * <li>les dates métier</li>
 * <li>le statut virtuel ou non (droit réel / droit virtuel)</li>
 * <li>le type de droit (droit de propriété / servitude)</li>
 * <li>le nombre d'ayants-droits</li>
 * <li>le nombre d'immeubles</li>
 * </ul>
 */
public class DroitRFRangeMetierComparator implements Comparator<DroitRF> {
	@Override
	public int compare(DroitRF o1, DroitRF o2) {
		int c = DateRangeComparator.compareRanges(o1.getRangeMetier(), o2.getRangeMetier());
		if (c != 0) {
			return c;
		}
		c = compareStatus(o1, o2);
		if (c != 0) {
			return c;
		}
		c = o1.getTypeDroit().compareTo(o2.getTypeDroit());
		if (c != 0) {
			return c;
		}
		c = compareList(o1.getAyantDroitList(), o2.getAyantDroitList(), AyantDroitRF::getId);
		if (c != 0) {
			return c;
		}
		c = compareList(o1.getImmeubleList(), o2.getImmeubleList(), ImmeubleRF::getId);
		if (c != 0) {
			return c;
		}
		return c;
	}

	private static int compareStatus(DroitRF o1, DroitRF o2) {
		final boolean o1Virtuel = o1 instanceof DroitVirtuelRF;
		final boolean o2Virtuel = o2 instanceof DroitVirtuelRF;

		if (o1Virtuel == o2Virtuel) {
			return 0;
		}
		else if (o1Virtuel) {
			return 1;   // les droits virtuels à la fin
		}
		else {
			return -1;
		}
	}

	/**
	 * Compare les deux listes selon les critères suivants :
	 * <ol>
	 * <li>leurs tailles</li>
	 * <li>leurs membres un à un</li>
	 * </ol>
	 *
	 * @param left         une liste
	 * @param right        une autre liste
	 * @param keyExtractor la fonction pour extraire la clé de comparaison d'un élément des listes
	 * @param <T>          le type des éléments des listes
	 * @param <U>          le type de la clé de comparaison des éléments des listes
	 * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
	 */
	private <T, U extends Comparable<? super U>> int compareList(List<? extends T> left, List<? extends T> right, Function<? super T, ? extends U> keyExtractor) {

		// on trie par taille des listes
		int c = Integer.compare(left.size(), right.size());
		if (c != 0) {
			return c;
		}

		// ensuite on compare membre-à-membre
		for (int i = 0; i < left.size(); i++) {
			final U lkey = keyExtractor.apply(left.get(i));
			final U rkey = keyExtractor.apply(right.get(i));
			c = lkey.compareTo(rkey);
			if (c != 0) {
				return c;
			}
		}

		return c;
	}
}
