package ch.vd.unireg.webservices.v7.cache;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

public class GetImmovablePropertyByLocationKey extends LandRegistryCacheKey {

	private int municipalityFsoId;
	private int parcelNumber; 
	@Nullable
	private Integer index1;
	@Nullable
	private Integer index2;
	@Nullable
	private Integer index3;

	public GetImmovablePropertyByLocationKey(int municipalityFsoId, int parcelNumber, @Nullable Integer index1, @Nullable Integer index2, @Nullable Integer index3) {
		this.municipalityFsoId = municipalityFsoId;
		this.parcelNumber = parcelNumber;
		this.index1 = index1;
		this.index2 = index2;
		this.index3 = index3;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final GetImmovablePropertyByLocationKey that = (GetImmovablePropertyByLocationKey) o;
		return municipalityFsoId == that.municipalityFsoId &&
				parcelNumber == that.parcelNumber &&
				Objects.equals(index1, that.index1) &&
				Objects.equals(index2, that.index2) &&
				Objects.equals(index3, that.index3);
	}

	@Override
	public int hashCode() {
		return Objects.hash(municipalityFsoId, parcelNumber, index1, index2, index3);
	}
}
