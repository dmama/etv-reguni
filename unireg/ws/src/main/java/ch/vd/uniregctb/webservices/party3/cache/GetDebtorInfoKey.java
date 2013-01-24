package ch.vd.uniregctb.webservices.party3.cache;

class GetDebtorInfoKey extends CacheKey {

	public final int taxPeriod;

	GetDebtorInfoKey(long debtorNumber, int taxPeriod) {
		super(debtorNumber);
		this.taxPeriod = taxPeriod;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		final GetDebtorInfoKey that = (GetDebtorInfoKey) o;
		return taxPeriod == that.taxPeriod;

	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + taxPeriod;
		return result;
	}
}
