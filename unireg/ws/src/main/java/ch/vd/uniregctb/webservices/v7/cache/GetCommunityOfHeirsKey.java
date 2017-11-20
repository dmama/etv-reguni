package ch.vd.uniregctb.webservices.v7.cache;

import java.util.Objects;

public class GetCommunityOfHeirsKey {

	private final int deceasedId;

	public GetCommunityOfHeirsKey(int deceasedId) {
		this.deceasedId = deceasedId;
	}

	public int getDeceasedId() {
		return deceasedId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final GetCommunityOfHeirsKey that = (GetCommunityOfHeirsKey) o;
		return deceasedId == that.deceasedId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(deceasedId);
	}
}
