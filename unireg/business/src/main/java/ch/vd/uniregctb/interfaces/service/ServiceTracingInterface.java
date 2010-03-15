package ch.vd.uniregctb.interfaces.service;

import java.util.Map;

public interface ServiceTracingInterface {

	/**
	 * @return le timestamp (nanoseconces) du dernier appel effectué sur le service.
	 */
	public abstract long getLastCallTime();

	/**
	 * @return le temps total passé dans le service depuis le démarrage de l'application
	 */
	public abstract long getTotalTime();

	/**
	 * @return le ping moyen du service depuis le démarrage de l'application
	 */
	public abstract long getTotalPing();

	/**
	 * @return le temps récent (les 5 dernières minutes d'activité) passé dans le service
	 */
	public abstract long getRecentTime();

	/**
	 * @return le ping moyen récent (les 5 dernières minutes d'activité) passé dans le service
	 */
	public abstract long getRecentPing();

	/**
	 * @return les données détaillée par sous-systèmes, ou <i>null</i> si ces informations ne sont pas disponibles.
	 */
	public Map<String, ? extends ServiceTracingInterface> getDetailedData();
}
