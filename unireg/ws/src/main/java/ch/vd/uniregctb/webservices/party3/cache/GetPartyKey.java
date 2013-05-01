package ch.vd.uniregctb.webservices.party3.cache;

/**
 * Clé utilisée pour indexer les tiers stockés dans le cache. Cette clé ne contient pas les 'parts'.
 */
class GetPartyKey extends CacheKey {

	public GetPartyKey(long partyNumber) {
		super(partyNumber);
	}
}
