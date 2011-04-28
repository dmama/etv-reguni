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
	private final Long totalPing;
	private final Long totalTime;
	private final Long totalCount;
	private final Long recentPing;
	private final Long recentTime;
	private final Long recentCount;
	private final Map<String, ServiceStats> detailedData = new HashMap<String, ServiceStats>();

	public ServiceStats(ServiceTracingInterface rawService) {
		this.lastCallTime = rawService.getLastCallTime();
		this.totalPing = rawService.getTotalPing();
		this.totalTime = rawService.getTotalTime();
		this.totalCount = rawService.getTotalCount();
		this.recentPing = rawService.getRecentPing();
		this.recentTime = rawService.getRecentTime();
		this.recentCount = rawService.getRecentCount();

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
	public Long getTotalPing() {
		return totalPing;
	}

	/**
	 * @return le temps total passé dans le service depuis le démarrage de l'application
	 */
	public Long getTotalTime() {
		return totalTime;
	}

	public Long getTotalCount() {
		return totalCount;
	}

	/**
	 * @return le ping moyen récent (les 5 dernières minutes d'activité) passé dans le service
	 */
	public Long getRecentPing() {
		return recentPing;
	}

	/**
	 * @return le temps récent (les 5 dernières minutes d'activité) passé dans le service
	 */
	public Long getRecentTime() {
		return recentTime;
	}

	public Long getRecentCount() {
		return recentCount;
	}

	/**
	 * @return les données détaillée par sous-systèmes, ou <i>null</i> si ces informations ne sont pas disponibles.
	 */
	public Map<String, ServiceStats> getDetailedData() {
		return detailedData;
	}
}