package ch.vd.unireg.webservices.v7.cache;

import java.util.Objects;

public class GetImmovablePropertyKey extends LandRegistryCacheKey {
	private final long immoId;

	public GetImmovablePropertyKey(long immoId) {
		this.immoId = immoId;
	}

	public long getImmoId() {
		return immoId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final GetImmovablePropertyKey that = (GetImmovablePropertyKey) o;
		return immoId == that.immoId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(immoId);
	}
}
