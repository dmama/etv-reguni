package ch.vd.uniregctb.common;

import org.jetbrains.annotations.NotNull;

/**
 * Comparateur de données qui peuvent être nulles, selon leur ordre naturel
 * @param <T> le type de donnée
 * @see Comparable
 */
public class NullableDefaultComparator<T extends Comparable<T>> extends NullableComparator<T> {

	public NullableDefaultComparator(boolean nullAtEnd) {
		super(nullAtEnd);
	}

	@Override
	protected int compareNonNull(@NotNull T o1, @NotNull T o2) {
		return o1.compareTo(o2);
	}
}
