package ch.vd.uniregctb.common;

import java.util.Comparator;

import org.jetbrains.annotations.NotNull;

/**
 * Comparateur de données qui peuvent être nulles
 * @param <T> le type de la donnée
 */
public abstract class NullableComparator<T> implements Comparator<T> {

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
