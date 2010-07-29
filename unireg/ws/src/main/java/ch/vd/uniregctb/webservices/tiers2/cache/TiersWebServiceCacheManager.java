package ch.vd.uniregctb.webservices.tiers2.cache;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.data.DataEventListener;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.Tiers;
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

		// on récupère tous les ids des tiers impactés par le changement sur le tiers passé en paramètre
		final Set<Long> ids = new HashSet<Long>();
		fillRelatedIds(id, ids, 0);

		// on les évicte tous, sans rémission possible
		for (Long i : ids) {
			cache.evictTiers(i);
		}
	}

	private void fillRelatedIds(long id, Set<Long> ids, int callDepth) {
		
		if (!ids.add(id)) {
			// l'id existe déjà, pas besoin d'aller plus loin
			return;
		}

		if (callDepth > 2) {
			// on ne va pas plus loin que deux niveaux de rapports : il ne devrait pas y avoir de situation où c'est nécessaire
			return;
		}

		final Tiers tiers = tiersDAO.get(id);
		if (tiers != null) {
			final Set<RapportEntreTiers> rapportsObjet = tiers.getRapportsObjet();
			if (rapportsObjet != null) {
				for (RapportEntreTiers r : rapportsObjet) {
					if (r instanceof RapportPrestationImposable) {
						continue;
					}
					fillRelatedIds(r.getSujetId(), ids, callDepth + 1);
				}
			}
			final Set<RapportEntreTiers> rapportsSujet = tiers.getRapportsSujet();
			if (rapportsSujet != null) {
				for (RapportEntreTiers r : rapportsSujet) {
					if (r instanceof RapportPrestationImposable) {
						continue;
					}
					fillRelatedIds(r.getObjetId(), ids, callDepth + 1);
				}
			}
		}
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
			onTiersChange(pp.getNumero());
		}
	}

	public void afterPropertiesSet() throws Exception {
		dataEventService.register(this);
	}
}
