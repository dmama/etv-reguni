package ch.vd.uniregctb.webservices.tiers3.cache;

/**
 * Clé utilisée pour indexer les tiers stockés dans le cache. Cette clé ne contient pas les 'parts'.
 */
class GetTiersKey extends CacheKey {

	public GetTiersKey(long tiersNumber) {
		super(tiersNumber);
	}
}
