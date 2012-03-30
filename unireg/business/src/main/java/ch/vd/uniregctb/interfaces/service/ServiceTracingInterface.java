package ch.vd.uniregctb.interfaces.service;

import java.util.Map;

public interface ServiceTracingInterface {

	/**
	 * @return le timestamp (nanosecondes) du dernier appel effectué sur le service.
	 */
	long getLastCallTime();

	/**
	 * @return le temps total (nanosecondes) passé dans le service depuis le démarrage de l'application
	 */
	long getTotalTime();

	/**
	 * @return le nombre d'appel total au service depuis le démarrage de l'application
	 */
	long getTotalCount();

	/**
	 * @return le ping moyen (millisecondes) du service depuis le démarrage de l'application
	 */
	long getTotalPing();

	/**
	 * @return le nombre total d'éléments pris en compte depuis le démarrage de l'application
	 */
	long getTotalItemsCount();

	/**
	 * @return le ping moyen (millisecondes) des éléments pris en compte depuis le démarrage de l'application
	 */
	long getTotalItemsPing();

	/**
	 * @return le temps récent (nanosecondes) (les 5 dernières minutes d'activité) passé dans le service
	 */
	long getRecentTime();

	/**
	 * @return le ping moyen récent (millisecondes) (les 5 dernières minutes d'activité) passé dans le service
	 */
	long getRecentPing();

	/**
	 * @return le nombre d'appel récent (les 5 dernières minutes d'activité) au service
	 */
	long getRecentCount();

	/**
	 * @return le nombre d'éléments récemment pris en compte
	 */
	long getRecentItemsCount();

	/**
	 * @return le ping moyen (millisecondes) des éléments récemment pris en compte
	 */
	long getRecentItemsPing();
	
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
