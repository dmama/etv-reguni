package ch.vd.uniregctb.webservices.tiers3.cache;

public class GetPartyTypeKey {

	private long partyId;

	public GetPartyTypeKey(long partyId) {
		this.partyId = partyId;
	}

	public long getPartyId() {
		return partyId;
	}

	public void setPartyId(long partyId) {
		this.partyId = partyId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final GetPartyTypeKey that = (GetPartyTypeKey) o;
		return partyId == that.partyId;

	}

	@Override
	public int hashCode() {
		return (int) (partyId ^ (partyId >>> 32));
	}
}
