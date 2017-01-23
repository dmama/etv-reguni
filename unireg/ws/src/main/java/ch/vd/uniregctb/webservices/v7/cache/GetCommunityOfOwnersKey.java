package ch.vd.uniregctb.webservices.v7.cache;

import java.util.Objects;

public class GetCommunityOfOwnersKey extends LandRegistryCacheKey {
	private final long communityId;

	public GetCommunityOfOwnersKey(long communityId) {
		this.communityId = communityId;
	}

	public long getCommunityId() {
		return communityId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final GetCommunityOfOwnersKey that = (GetCommunityOfOwnersKey) o;
		return communityId == that.communityId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(communityId);
	}
}
