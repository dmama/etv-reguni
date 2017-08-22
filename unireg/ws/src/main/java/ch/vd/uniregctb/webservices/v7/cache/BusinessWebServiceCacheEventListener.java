package ch.vd.uniregctb.webservices.v7.cache;

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
	public void onImmeubleChange(long immeubleId) {
		cache.evictImmovableProperty(immeubleId);
	}

	@Override
	public void onBatimentChange(long batimentId) {
		cache.evictBuilding(batimentId);
	}

	@Override
	public void onCommunauteChange(long communauteId) {
		cache.evictCommunityOfOwners(communauteId);
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
