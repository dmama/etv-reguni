package ch.vd.uniregctb.webservices.v7.cache;

import java.util.Objects;

public class GetImmovablePropertyKey extends LandRegistryCacheKey {
	private final long immId;

	public GetImmovablePropertyKey(long immId) {
		this.immId = immId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final GetImmovablePropertyKey that = (GetImmovablePropertyKey) o;
		return immId == that.immId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(immId);
	}
}
