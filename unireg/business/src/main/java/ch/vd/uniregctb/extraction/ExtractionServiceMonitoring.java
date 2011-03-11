package ch.vd.uniregctb.extraction;

import java.util.List;

/**
 * Données du service d'extractions accessibles à des fins de monitoring
 */
public interface ExtractionServiceMonitoring {

	/**
	 * @return Nombre de jour après la fin de l'exécution de l'extraction pendant lesquels le résultat est conservé
	 */
	int getDelaiExpirationEnJours();

	/**
	 * @return Nombre d'exécuteurs en parallèle
	 */
	int getNbExecutors();

	/**
	 * @param visa (optionnel) ne fournit que les jobs demandé par le visa donné
	 * @return Extraction du contenu de la queue des demandes en attente
	 */
	List<ExtractionJob> getQueueContent(String visa);

	/**
	 * @return Nombre de demandes d'extraction actuellement en attente
	 */
	int getQueueSize();

	/**
	 * @return Nombre de demandes satisfaites depuis le démarrage du service
	 */
	int getNbExecutedQueries();

	/**
	 * @param visa (optionnel) ne fournit que les jobs demandé par le visa donné
	 * @return Extraction des jobs d'extraction actuellement en cours
	 */
	List<ExtractionJob> getExtractionsEnCours(String visa);
}
