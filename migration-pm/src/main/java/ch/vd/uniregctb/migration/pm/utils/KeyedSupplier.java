package ch.vd.uniregctb.migration.pm.utils;

import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

/**
 * Supplier qui connaît aussi le type d'objet
 * @param <T> type d'objet fourni
 */
public class KeyedSupplier<T> implements Supplier<T> {

	private final EntityKey key;
	private final Supplier<T> target;

	public KeyedSupplier(@NotNull EntityKey key, @NotNull Supplier<T> target) {
		this.key = key;
		this.target = target;
	}

	@Override
	public T get() {
		return target.get();
	}

	public EntityKey getKey() {
		return key;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final KeyedSupplier<?> that = (KeyedSupplier<?>) o;
		return key.equals(that.key);
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}
}
