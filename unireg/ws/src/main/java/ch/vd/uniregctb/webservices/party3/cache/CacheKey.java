package ch.vd.uniregctb.webservices.party3.cache;

public abstract class CacheKey {

	public final long partyNumber;

	public CacheKey(long partyNumber) {
		this.partyNumber = partyNumber;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (partyNumber ^ (partyNumber >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CacheKey other = (CacheKey) obj;
		if (partyNumber != other.partyNumber)
			return false;
		return true;
	}
}
