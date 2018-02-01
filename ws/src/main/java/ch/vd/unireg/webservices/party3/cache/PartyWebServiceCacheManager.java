package ch.vd.unireg.webservices.party3.cache;

import java.util.Set;

import ch.vd.unireg.webservices.common.WebServiceEventInterface;

/**
 * Cette classe maintient le cache du web-service cohérent en fonction des modifications apportées dans la base de données Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class PartyWebServiceCacheManager implements WebServiceEventInterface {

	private PartyWebServiceCache cache;

	public void setCache(PartyWebServiceCache cache) {
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
		// rien à faire, les immeubles n'existent pas dans le v3
	}

	@Override
	public void onBatimentChange(long batimentId) {
		// rien à faire, les bâtiments n'existent pas dans le v3
	}

	@Override
	public void onCommunauteChange(long communauteId) {
		// rien à faire, les communautés de propriétaires n'existaient pas dans le v3
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
