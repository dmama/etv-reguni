package ch.vd.uniregctb.webservices.tiers.cache;

import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.database.DatabaseListener;
import ch.vd.uniregctb.database.DatabaseService;
import ch.vd.uniregctb.interfaces.service.CivilListener;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;

/**
 * Cette classe maintient le cache du web-service cohérent en fonction des modifications apportées dans la base de données Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class TiersWebServiceCacheManager implements DatabaseListener, CivilListener, InitializingBean {

	private DatabaseService dbService;
	private TiersWebServiceCache cache;
	private TiersDAO tiersDAO;

	public void setDbService(DatabaseService dbService) {
		this.dbService = dbService;
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
		dbService.register(this);
	}
}
