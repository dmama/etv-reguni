package ch.vd.uniregctb.webservices.tiers2.cache;

import ch.vd.uniregctb.webservices.tiers2.data.Date;

/**
 * Clé utilisée pour indexer les tiers stockés dans le cache. Cette clé ne contient pas les 'parts'.
 */
class GetTiersKey extends CacheKey {

	public final Date date;

	public GetTiersKey(long tiersNumber, Date date) {
		super(tiersNumber);
		this.date = date;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((date == null) ? 0 : date.hashCode());
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
		GetTiersKey other = (GetTiersKey) obj;
		if (date == null) {
			if (other.date != null)
				return false;
		}
		else if (!date.equals(other.date))
			return false;
		return true;
	}
}