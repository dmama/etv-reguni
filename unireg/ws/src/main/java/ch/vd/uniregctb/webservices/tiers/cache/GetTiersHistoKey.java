package ch.vd.uniregctb.webservices.tiers.cache;

/**
 * Clé utilisée pour indexer les tiers stockés dans le cache. Cette clé ne contient pas les 'parts'.
 */
class GetTiersHistoKey extends CacheKey {

	public GetTiersHistoKey(long tiersNumber) {
		super(tiersNumber);
	}
}