package ch.vd.uniregctb.webservices.tiers2.cache;

import org.apache.commons.lang3.StringUtils;

/**
 * Clé utilisée pour indexer les tiers stockés dans le cache. Cette clé ne contient pas les 'parts'.
 */
class GetTiersHistoKey extends CacheKey {

	public GetTiersHistoKey(long tiersNumber) {
		super(tiersNumber);
	}

	@Override
	protected String toStringPart() {
		return StringUtils.EMPTY;
	}
}