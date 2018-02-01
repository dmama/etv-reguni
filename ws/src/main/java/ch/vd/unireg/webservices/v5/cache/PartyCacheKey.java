package ch.vd.unireg.webservices.v5.cache;

/**
 * Classe de base des clés de cache associée à un tiers
 */
abstract class PartyCacheKey {

	public final long partyNo;

	protected PartyCacheKey(long partyNo) {
		this.partyNo = partyNo;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final PartyCacheKey that = (PartyCacheKey) o;
		return partyNo == that.partyNo;

	}

	@Override
	public int hashCode() {
		return (int) (partyNo ^ (partyNo >>> 32));
	}

	@Override
	public String toString() {
		return String.format("%s{partyNo=%d%s}", getClass().getSimpleName(), partyNo, toStringPart());
	}

	protected abstract String toStringPart();
}
