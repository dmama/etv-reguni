package ch.vd.uniregctb.webservices.tiers2.cache;

class GetDebiteurInfoKey extends CacheKey {

	public int periodeFiscale;

	GetDebiteurInfoKey(long tiersNumber, int periodeFiscale) {
		super(tiersNumber);
		this.periodeFiscale = periodeFiscale;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		final GetDebiteurInfoKey that = (GetDebiteurInfoKey) o;
		return periodeFiscale == that.periodeFiscale;

	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + periodeFiscale;
		return result;
	}
}
