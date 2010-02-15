package ch.vd.uniregctb.webservices.tiers.cache;

/**
 * Clé utilisée pour indexer les tiers stockés dans le cache. Cette clé ne contient pas les 'parts'.
 */
class GetTiersPeriodeKey extends CacheKey {

	public int periode;

	public GetTiersPeriodeKey(long tiersNumber, int periode) {
		super(tiersNumber);
		this.periode = periode;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + periode;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		GetTiersPeriodeKey other = (GetTiersPeriodeKey) obj;
		if (periode != other.periode)
			return false;
		return true;
	}
}
