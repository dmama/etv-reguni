package ch.vd.uniregctb.webservices.tiers2.cache;

import java.util.Set;

import ch.vd.uniregctb.webservices.common.WebServiceEventInterface;

/**
 * Cette classe maintient le cache du web-service cohérent en fonction des modifications apportées dans la base de données Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class TiersWebServiceCacheManager implements WebServiceEventInterface {

	private TiersWebServiceCache cache;

	public void setCache(TiersWebServiceCache cache) {
		this.cache = cache;
	}

	@Override
	public void onTiersChange(Set<Long> ids) {
		for (Long id : ids) {
			cache.evictTiers(id);
		}
	}

	@Override
	public void onTruncateDatabase() {
		cache.clearAll();
	}

	@Override
	public void onLoadDatabase() {
		// rien à faire
	}
}
