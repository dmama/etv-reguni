package ch.vd.uniregctb.webservices.party3.cache;

import org.apache.commons.lang3.StringUtils;

/**
 * Clé utilisée pour indexer les tiers stockés dans le cache. Cette clé ne contient pas les 'parts'.
 */
class GetPartyKey extends CacheKey {

	public GetPartyKey(long partyNumber) {
		super(partyNumber);
	}

	@Override
	protected String toStringPart() {
		return StringUtils.EMPTY;
	}
}
