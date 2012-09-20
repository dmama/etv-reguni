package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;

/**
 * Classe qui regroupe quelques classes et méthodes utiles dans la comparaison d'invididus civils
 */
public abstract class IndividuComparisonHelper {

	/**
	 * Comparateur de données qui peuvent être nulles
	 * @param <T> le type de la donnée
	 */
	public static abstract class NullableComparator<T> implements Comparator<T> {

		private final boolean nullAtEnd;

		public NullableComparator(boolean nullAtEnd) {
			this.nullAtEnd = nullAtEnd;
		}

		@Override
		public final int compare(T o1, T o2) {
			if (o1 == o2) {
				return 0;
			}
			else if (o1 == null) {
				return nullAtEnd ? 1 : -1;
			}
			else if (o2 == null) {
				return nullAtEnd ? -1 : 1;
			}
			else {
				return compareNonNull(o1, o2);
			}
		}

		protected abstract int compareNonNull(@NotNull T o1, @NotNull T o2);
	}

	/**
	 * Comparateur de données qui peuvent être nulles, selon leur ordre naturel
	 * @param <T> le type de donnée
	 * @see Comparable
	 */
	public static class DefaultComparator<T extends Comparable<T>> extends NullableComparator<T> {

		public DefaultComparator(boolean nullAtEnd) {
			super(nullAtEnd);
		}

		@Override
		protected int compareNonNull(@NotNull T o1, @NotNull T o2) {
			return o1.compareTo(o2);
		}
	}

	public static final Comparator<DateRange> RANGE_COMPARATOR = new NullableComparator<DateRange>(true) {
		@Override
		protected int compareNonNull(@NotNull DateRange o1, @NotNull DateRange o2) {
			return DateRangeComparator.compareRanges(o1, o2);
		}
	};

	public static final Comparator<Integer> INTEGER_COMPARATOR = new DefaultComparator<Integer>(true);

	/**
	 * Interface de vérification d'égalité
	 * @param <T> type de la donnée à vérifier
	 */
	public static interface Equalator<T> {
		boolean areEqual(T o1, T o2);
	}

	/**
	 * Vérificateur d'égalité de données qui peuvent être nulles
	 * @param <T> type de la donnée à vérifier
	 */
	public static abstract class NullableEqualator<T> implements Equalator<T> {

		@Override
		public final boolean areEqual(T o1, T o2) {
			if (o1 == o2) {
				return true;
			}
			else if (o1 == null || o2 == null) {
				return false;
			}
			else {
				return areNonNullEqual(o1, o2);
			}
		}

		protected abstract boolean areNonNullEqual(@NotNull T o1, @NotNull T o2);
	}

	/**
	 * Vérificateur d'égalité de données qui peuvent être nulles, basé sur l'appel à la méthode {@link Object#equals(Object)}
	 * @param <T> type de la donnée à vérifier
	 */
	public static class DefaultEqualator<T> extends NullableEqualator<T> {
		protected boolean areNonNullEqual(@NotNull T o1, @NotNull T o2) {
			return o1.equals(o2);
		}
	}

	public static final Equalator<DateRange> RANGE_EQUALATOR = new NullableEqualator<DateRange>() {
		@Override
		protected boolean areNonNullEqual(@NotNull DateRange o1, @NotNull DateRange o2) {
			return DateRangeHelper.equals(o1, o2);
		}
	};

	public static final Equalator<Integer> INTEGER_EQUALATOR = new DefaultEqualator<Integer>();

	/**
	 * @param c1 collection 1
	 * @param c2 collection 2
	 * @param comparator comparateur utilisé pour trier les collections et s'affranchir de l'ordre des éléments dans les collections
	 * @param equalator vérificateur utilisé pour vérifier l'égalité des objets entre les deux collections
	 * @param <T> type de contenu des collections à vérifier
	 * @return <code>true/code> si les deux collections contiennent les mêmes éléments (même dans le désordre)
	 */
	public static <T> boolean areContentsEqual(Collection<T> c1, Collection<T> c2, Comparator<T> comparator, Equalator<T> equalator) {
		if (c1 == c2) {
			return true;
		}
		else if ((c1 != null ? c1.size() : 0) != (c2 != null ? c2.size() : 0)) {
			return false;
		}
		else {
			final List<T> sl1 = new ArrayList<T>(c1 != null ? c1 : Collections.<T>emptyList());
			Collections.sort(sl1, comparator);

			final List<T> sl2 = new ArrayList<T>(c2 != null ? c2 : Collections.<T>emptyList());
			Collections.sort(sl2, comparator);

			for (int i = 0 ; i < sl1.size() ; ++ i) {
				final T o1 = sl1.get(i);
				final T o2 = sl2.get(i);
				if (o1 != o2 && (o1 == null || o2 == null)) {
					return false;
				}
				else if (!equalator.areEqual(o1, o2)) {
					return false;
				}
			}
			return true;
		}
	}
}
