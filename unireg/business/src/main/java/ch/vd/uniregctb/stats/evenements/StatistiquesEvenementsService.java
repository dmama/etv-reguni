package ch.vd.uniregctb.stats.evenements;

import ch.vd.registre.base.date.RegDate;

/**
 * Interface du service utilisé pour l'établissement de statistiques
 * autours des événements
 */
public interface StatistiquesEvenementsService {

	/**
	 * Renvoie les statistiques sur les événements civils issus de RegPP
	 * @return les données nécessaires à l'établissement d'un rapport
	 */
	StatsEvenementsCivilsRegPPResults getStatistiquesEvenementsCivilsRegPP();

	/**
	 * Renvoie les statistiques sur les événements civils issus de RCPers (événements basés e-CH)
	 * @param debutActivite date à partir de laquelle on liste les modifications récentes
	 * @return les données nécessaires à l'établissement d'un rapport
	 */
	StatsEvenementsCivilsEchResults getStatistiquesEvenementsCivilsEch(RegDate debutActivite);

	/**
	 * Renvoie les statistiques sur les événements externes
	 * @return les données nécessaires à l'établissement d'un rapport
	 */
	StatsEvenementsExternesResults getStatistiquesEvenementsExternes();

	/**
	 * Renvoie les statistiques sur les événements de demande d'identification de contribuable
	 * @return les données nécessaires à l'établissement d'un rapport
	 * @param debutActivite date à partir de laquelle on liste les modifications récentes
	 */
	StatsEvenementsIdentificationContribuableResults getStatistiquesEvenementsIdentificationContribuable(RegDate debutActivite);
}
