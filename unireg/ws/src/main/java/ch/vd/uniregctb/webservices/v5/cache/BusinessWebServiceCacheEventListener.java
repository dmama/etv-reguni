package ch.vd.uniregctb.webservices.v5.cache;

import java.util.Set;

import ch.vd.uniregctb.webservices.common.WebServiceEventInterface;

public class BusinessWebServiceCacheEventListener implements WebServiceEventInterface {

	private BusinessWebServiceCache cache;

	public void setCache(BusinessWebServiceCache cache) {
		this.cache = cache;
	}

	@Override
	public void onTiersChange(Set<Long> ids) {
		for (Long id : ids) {
			cache.evictParty(id);
		}
	}

	@Override
	public void onTruncateDatabase() {
		cache.reset();
	}

	@Override
	public void onLoadDatabase() {
		// rien de particulier à faire
	}
}
