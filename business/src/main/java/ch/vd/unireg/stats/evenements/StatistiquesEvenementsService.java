package ch.vd.unireg.stats.evenements;

import ch.vd.registre.base.date.RegDate;

/**
 * Interface du service utilisé pour l'établissement de statistiques
 * autours des événements
 */
public interface StatistiquesEvenementsService {

	/**
	 * Renvoie les statistiques sur les événements civils issus de RCPers (personnes)
	 * @param debutActivite date à partir de laquelle on liste les modifications récentes
	 * @return les données nécessaires à l'établissement d'un rapport
	 */
	StatsEvenementsCivilsPersonnesResults getStatistiquesEvenementsCivilsPersonnes(RegDate debutActivite);

	/**
	 * Renvoie les statistiques sur les événements civils issus de RCEnt (entreprises)
	 * @param debutActivite date à partir de laquelle on liste les modifications récentes
	 * @return les données nécessaires à l'établissement d'un rapport
	 */
	StatsEvenementsCivilsEntreprisesResults getStatistiquesEvenementsCivilsEntreprises(RegDate debutActivite);

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

	/**
	 * Renvoie les statistiques sur les unités de traitement des événements ReqDes (notaires)
	 * @param debutActivite date à partir de laquelle on liste des modifications récentes
	 * @return les données nécessaire à l'établissement du rapport
	 */
	StatsEvenementsNotairesResults getStatistiquesEvenementsNotaires(RegDate debutActivite);
}
