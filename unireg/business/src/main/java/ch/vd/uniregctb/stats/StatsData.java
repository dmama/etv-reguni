package ch.vd.uniregctb.stats;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.interfaces.service.ServiceTracingInterface;

/**
 * Contient les statistiques de fonctionnement d'un service.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class StatsData {

	private long lastCallTime;
	private Long totalPing;
	private Long totalTime;
	private Long recentPing;
	private Long recentTime;
	private Long hitsPercent;
	private Long hitsCount;
	private Long totalCount;
	private Map<String, StatsData> detailedData = new HashMap<String, StatsData>();

	public StatsData() {
	}

	public StatsData(ServiceTracingInterface rawService) {
		this.lastCallTime = rawService.getLastCallTime();
		this.totalPing = rawService.getTotalPing();
		this.totalTime = rawService.getTotalTime();
		this.recentPing = rawService.getRecentPing();
		this.recentTime = rawService.getRecentTime();

		final Map<String, ? extends ServiceTracingInterface> map = rawService.getDetailedData();
		if (map != null) {
			for (Map.Entry<String, ? extends ServiceTracingInterface> e : map.entrySet()) {
				final StatsData d = new StatsData(e.getValue());
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

	public void setLastCallTime(long lastCallTime) {
		this.lastCallTime = lastCallTime;
	}

	/**
	 * @return le ping moyen du service depuis le démarrage de l'application
	 */
	public Long getTotalPing() {
		return totalPing;
	}

	public void setTotalPing(Long ping) {
		this.totalPing = ping;
	}

	/**
	 * @return le temps total passé dans le service depuis le démarrage de l'application
	 */
	public Long getTotalTime() {
		return totalTime;
	}

	public void setTotalTime(Long totalTime) {
		this.totalTime = totalTime;
	}

	/**
	 * @return le ping moyen récent (les 5 dernières minutes d'activité) passé dans le service
	 */
	public Long getRecentPing() {
		return recentPing;
	}

	public void setRecentPing(Long recentPing) {
		this.recentPing = recentPing;
	}

	/**
	 * @return le temps récent (les 5 dernières minutes d'activité) passé dans le service
	 */
	public Long getRecentTime() {
		return recentTime;
	}

	public void setRecentTime(Long recentTime) {
		this.recentTime = recentTime;
	}

	/**
	 * @return le percentage de hits sur le cache du service
	 */
	public Long getHitsPercent() {
		return hitsPercent;
	}

	public void setHitsPercent(Long hitsPercent) {
		this.hitsPercent = hitsPercent;
	}

	/**
	 * @return le nombre de hits sur le cache du service
	 */
	public Long getHitsCount() {
		return hitsCount;
	}

	public void setHitsCount(Long hitsCount) {
		this.hitsCount = hitsCount;
	}

	/**
	 * @return le nombre d'appels sur le cache du service
	 */
	public Long getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(Long totalCount) {
		this.totalCount = totalCount;
	}

	/**
	 * @return les données détaillée par sous-systèmes, ou <i>null</i> si ces informations ne sont pas disponibles.
	 */
	public Map<String, StatsData> getDetailedData() {
		return detailedData;
	}

	public void setDetailedData(Map<String, StatsData> detailedData) {
		this.detailedData = detailedData;
	}
}
