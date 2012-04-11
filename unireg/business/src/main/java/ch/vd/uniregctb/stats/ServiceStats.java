package ch.vd.uniregctb.stats;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.interfaces.service.ServiceTracingInterface;

/**
 * Contient les statistiques de fonctionnement d'un service.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ServiceStats {

	private final long lastCallTime;
	private final long totalPing;
	private final long totalTime;
	private final long totalCount;
	private final Long totalItemsCount;
	private final Long totalItemsPing;
	private final long recentPing;
	private final long recentTime;
	private final long recentCount;
	private final Long recentItemsCount;
	private final Long recentItemsPing;
	private final Map<String, ServiceStats> detailedData = new HashMap<String, ServiceStats>();

	public ServiceStats(ServiceTracingInterface rawService) {
		this.lastCallTime = rawService.getLastCallTime();
		this.totalPing = rawService.getTotalPing();
		this.totalTime = rawService.getTotalTime();
		this.totalCount = rawService.getTotalCount();

		if (rawService.getTotalItemsCount() > rawService.getTotalCount()) {
			this.totalItemsCount = rawService.getTotalItemsCount();
			this.totalItemsPing = rawService.getTotalItemsPing();
		}
		else {
			this.totalItemsCount = null;
			this.totalItemsPing = null;
		}

		this.recentPing = rawService.getRecentPing();
		this.recentTime = rawService.getRecentTime();
		this.recentCount = rawService.getRecentCount();

		if (rawService.getRecentItemsCount() > rawService.getRecentCount()) {
			this.recentItemsCount = rawService.getRecentItemsCount();
			this.recentItemsPing = rawService.getRecentItemsPing();
		}
		else {
			this.recentItemsCount = null;
			this.recentItemsPing = null;
		}

		final Map<String, ? extends ServiceTracingInterface> map = rawService.getDetailedData();
		if (map != null) {
			for (Map.Entry<String, ? extends ServiceTracingInterface> e : map.entrySet()) {
				final ServiceStats d = new ServiceStats(e.getValue());
				detailedData.put(e.getKey(), d);
			}
		}
	}

	/**
	 * @return le timestamp (nanosecondes) du dernier appel au service
	 */
	public long getLastCallTime() {
		return lastCallTime;
	}

	/**
	 * @return le ping moyen du service depuis le démarrage de l'application
	 */
	public long getTotalPing() {
		return totalPing;
	}

	/**
	 * @return le temps total passé dans le service depuis le démarrage de l'application
	 */
	public long getTotalTime() {
		return totalTime;
	}

	public long getTotalCount() {
		return totalCount;
	}

	/**
	 * @return le nombre totale d'éléments demandés ou retournés (selon la définition du service)
	 */
	public Long getTotalItemsCount() {
		return totalItemsCount;
	}

	/**
	 * @return le ping moyen par élément demandé ou retourné.
	 */
	public Long getTotalItemsPing() {
		return totalItemsPing;
	}

	/**
	 * @return le ping moyen récent (les 5 dernières minutes d'activité) passé dans le service
	 */
	public long getRecentPing() {
		return recentPing;
	}

	/**
	 * @return le temps récent (les 5 dernières minutes d'activité) passé dans le service
	 */
	public long getRecentTime() {
		return recentTime;
	}

	public long getRecentCount() {
		return recentCount;
	}

	/**
	 * @return le nombre d'éléments récemment demandés ou retournés.
	 */
	public Long getRecentItemsCount() {
		return recentItemsCount;
	}

	/**
	 * @return le ping moyen par élément récemment demandé ou retourné.
	 */
	public Long getRecentItemsPing() {
		return recentItemsPing;
	}

	/**
	 * @return les données détaillée par sous-systèmes, ou <i>null</i> si ces informations ne sont pas disponibles.
	 */
	public Map<String, ServiceStats> getDetailedData() {
		return detailedData;
	}
}