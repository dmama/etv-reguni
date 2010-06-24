package ch.vd.uniregctb.webservices.tiers.cache;

import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.data.DataEventListener;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;

/**
 * Cette classe maintient le cache du web-service cohérent en fonction des modifications apportées dans la base de données Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class TiersWebServiceCacheManager implements DataEventListener, InitializingBean {

	private DataEventService dataEventService;
	private TiersWebServiceCache cache;
	private TiersDAO tiersDAO;

	public void setDataEventService(DataEventService dataEventService) {
		this.dataEventService = dataEventService;
	}

	public void setCache(TiersWebServiceCache cache) {
		this.cache = cache;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void onTiersChange(long id) {
		cache.evictTiers(id);
	}

	public void onDroitAccessChange(long tiersId) {
		// rien à faire
	}

	public void onTruncateDatabase() {
		cache.clearAll();
	}

	public void onLoadDatabase() {
		// rien à faire
	}

	public void onIndividuChange(long numero) {
		final PersonnePhysique pp = tiersDAO.getPPByNumeroIndividu(numero, true);
		if (pp != null) {
			cache.evictTiers(pp.getNumero());
		}
	}

	public void afterPropertiesSet() throws Exception {
		dataEventService.register(this);
	}
}
