package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;

/**
 * Classe utilitaire autour des éléments {@link Annulable}
 */
public abstract class AnnulableHelper {

	/**
	 * Comparateur qui en encapsule un autre en prenant en charge le fait que les éléments annulés viennent après les éléments non-annulés
	 * @param <T> type des éléments à comparer
	 */
	public static class AnnulesApresWrappingComparator<T extends Annulable> implements Comparator<T> {

		private final Comparator<? super T> wrapped;

		public AnnulesApresWrappingComparator(Comparator<? super T> wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public int compare(T o1, T o2) {
			if (o1.isAnnule() == o2.isAnnule()) {
				return wrapped.compare(o1, o2);
			}
			if (o1.isAnnule()) {
				return 1;
			}
			return -1;
		}
	}

	/**
	 * Comparateur capable de trier des {@link DateRange} {@link Annulable} en mettant systématiquement les éléments annulés à la fin,
	 * et en ordonnant les {@link DateRange} soit chronologiquement, soit à l'inverse
	 * @param <T> type des éléments comparés
	 */
	public static class AnnulableDateRangeComparator<T extends Annulable & DateRange> extends AnnulesApresWrappingComparator<T> {

		/**
		 * @param timeReversed <code>false</code> si les {@link DateRange} doivent être triés chronologiquement, <code>true</code> s'il faut les trier à l'inverse.
		 */
		public AnnulableDateRangeComparator(boolean timeReversed) {
			super(new DateRangeComparator<>(timeReversed ? DateRangeComparator.CompareOrder.DESCENDING : DateRangeComparator.CompareOrder.ASCENDING));
		}
	}

	/**
	 * @param col une collection d'éléments annulables
	 * @param <T> le type des éléments annuables
	 * @return une liste des seuls éléments non-annulés de la liste de départ (dans l'ordre de l'itérateur canonique de la collection fournie)
	 */
	@NotNull
	public static <T extends Annulable> List<T> sansElementsAnnules(Collection<T> col) {
		if (col == null || col.isEmpty()) {
			return Collections.emptyList();
		}
		return sansElementsAnnules(col.iterator(), col.size());
	}

	@NotNull
	private static <T extends Annulable> List<T> sansElementsAnnules(Iterator<T> iterator, int size) {
		final List<T> res = new ArrayList<>(size);
		while (iterator.hasNext()) {
			final T elt = iterator.next();
			if (!elt.isAnnule()) {
				res.add(elt);
			}
		}
		return res.size() == 0 ? Collections.emptyList() : res;
	}

	/**
	 * Méthode utilisable dans des constructions du type .filter(AnnulableHelper::nonAnnule)
	 * pour ne conserver que les éléments non-annulés d'un stream
	 * @param annulable l'entité à tester
	 * @return <code>true</code> si l'entité n'est pas annulée
	 */
	public static boolean nonAnnule(Annulable annulable) {
		return !annulable.isAnnule();
	}
}