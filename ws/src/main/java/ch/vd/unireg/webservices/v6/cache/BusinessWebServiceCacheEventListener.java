package ch.vd.unireg.webservices.v6.cache;

import java.util.Set;

import ch.vd.unireg.webservices.common.WebServiceEventInterface;

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
		// rien à faire, les immeubles n'existent pas dans le v6
	}

	@Override
	public void onBatimentChange(long batimentId) {
		// rien à faire, les bâtiments n'existent pas dans le v6
	}

	@Override
	public void onCommunauteChange(long communauteId) {
		// rien à faire, les communautés de propriétaires n'existaient pas dans le v6
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
