package ch.vd.uniregctb.migration.pm.historizer.container;

import org.jetbrains.annotations.NotNull;

public class DualKey<V1, V2> {

	@NotNull
	private final V1 v1;
	@NotNull
	private final V2 v2;

	public DualKey(@NotNull V1 v1, @NotNull V2 v2) {
		this.v1 = v1;
		this.v2 = v2;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final DualKey<?, ?> dualKey = (DualKey<?, ?>) o;

		if (!v1.equals(dualKey.v1)) return false;
		return v2.equals(dualKey.v2);

	}

	@Override
	public int hashCode() {
		int result = v1.hashCode();
		result = 31 * result + v2.hashCode();
		return result;
	}

	@NotNull
	public V1 getV1() {
		return v1;
	}

	@NotNull
	public V2 getV2() {
		return v2;
	}
}
