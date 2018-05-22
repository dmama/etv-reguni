package ch.vd.unireg.common;

import java.util.Comparator;
import java.util.List;

/**
 * Comparateur qui ordonne les éléments qui lui sont passés suivant le même
 * ordre que dans une liste fournie (ceux qui ne font pas partie de la
 * liste sont systématiquement placés à la fin du résultat trié)
 * @param <T>
 */
public final class GentilComparator<T> implements Comparator<T> {

	private final List<T> ordre;

	public GentilComparator(List<T> ordre) {
		this.ordre = ordre;
		if (ordre == null || ordre.isEmpty()) {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public int compare(T o1, T o2) {
		if (o1 == o2) {
			return 0;
		}
		else {
			final int idx1 = ordre.indexOf(o1);
			final int idx2 = ordre.indexOf(o2);
			if (idx1 == idx2) {
				return 0;
			}
			else if (idx1 == -1) {
				return 1;
			}
			else if (idx2 == -1) {
				return -1;
			}
			else {
				return Integer.compare(idx1 , idx2);
			}
		}
	}
}
