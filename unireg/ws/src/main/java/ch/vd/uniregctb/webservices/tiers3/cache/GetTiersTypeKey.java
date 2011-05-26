package ch.vd.uniregctb.webservices.tiers3.cache;

public class GetTiersTypeKey {

	private long tiersId;

	public GetTiersTypeKey(long tiersId) {
		this.tiersId = tiersId;
	}

	public long getTiersId() {
		return tiersId;
	}

	public void setTiersId(long tiersId) {
		this.tiersId = tiersId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final GetTiersTypeKey that = (GetTiersTypeKey) o;
		return tiersId == that.tiersId;

	}

	@Override
	public int hashCode() {
		return (int) (tiersId ^ (tiersId >>> 32));
	}
}
