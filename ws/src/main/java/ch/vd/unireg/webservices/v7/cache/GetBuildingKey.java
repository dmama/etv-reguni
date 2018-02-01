package ch.vd.unireg.webservices.v7.cache;

import java.util.Objects;

public class GetBuildingKey extends LandRegistryCacheKey {
	private final long buildingId;

	public GetBuildingKey(long buildingId) {
		this.buildingId = buildingId;
	}

	public long getBuildingId() {
		return buildingId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final GetBuildingKey that = (GetBuildingKey) o;
		return buildingId == that.buildingId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(buildingId);
	}
}
