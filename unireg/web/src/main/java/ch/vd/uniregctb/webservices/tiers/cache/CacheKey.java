package ch.vd.uniregctb.webservices.tiers.cache;

public abstract class CacheKey {

	public final long tiersNumber;

	public CacheKey(long tiersNumber) {
		this.tiersNumber = tiersNumber;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (tiersNumber ^ (tiersNumber >>> 32));
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
		if (tiersNumber != other.tiersNumber)
			return false;
		return true;
	}
}
