package ch.vd.uniregctb.interfaces.service;

import java.util.Map;

public interface ServiceTracingInterface {

	/**
	 * @return le timestamp (nanoseconces) du dernier appel effectué sur le service.
	 */
	long getLastCallTime();

	/**
	 * @return le temps total passé dans le service depuis le démarrage de l'application
	 */
	long getTotalTime();

	/**
	 * @return le nombre d'appel total au service depuis le démarrage de l'application
	 */
	long getTotalCount();

	/**
	 * @return le ping moyen du service depuis le démarrage de l'application
	 */
	long getTotalPing();

	/**
	 * @return le temps récent (les 5 dernières minutes d'activité) passé dans le service
	 */
	long getRecentTime();

	/**
	 * @return le ping moyen récent (les 5 dernières minutes d'activité) passé dans le service
	 */
	long getRecentPing();

	/**
	 * @return le nombre d'appel récent (les 5 dernières minutes d'activité) au service
	 */
	long getRecentCount();

	/**
	 * Appelé toutes les minutes pour tenir compte de l'avancement du temps dans les données relatives aux appels "récents"
	 * (c'est le bon moment pour "faire glisser" les données)
	 */
	void onTick();

	/**
	 * @return les données détaillée par sous-systèmes, ou <i>null</i> si ces informations ne sont pas disponibles.
	 */
	public Map<String, ? extends ServiceTracingInterface> getDetailedData();
}
