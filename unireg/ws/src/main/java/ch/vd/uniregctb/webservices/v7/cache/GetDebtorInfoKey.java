package ch.vd.uniregctb.webservices.v7.cache;

final class GetDebtorInfoKey extends PartyCacheKey {

	public final int pf;

	GetDebtorInfoKey(long partyNo, int pf) {
		super(partyNo);
		this.pf = pf;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		final GetDebtorInfoKey that = (GetDebtorInfoKey) o;
		return pf == that.pf;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + pf;
		return result;
	}

	@Override
	protected String toStringPart() {
		return String.format(", pf=%d", pf);
	}
}
