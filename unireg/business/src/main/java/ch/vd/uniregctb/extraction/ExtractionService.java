package ch.vd.uniregctb.extraction;

import java.util.List;

/**
 * Service asynchrone d'extraction de données (génération de gros CSV par exemple) - les résulats sont poussés dans le service d'inbox
 * @see ch.vd.uniregctb.inbox.InboxService
 */
public interface ExtractionService {

	/**
	 * Poste une demande d'exécution de l'extracteur donné pour le compte de l'utilisateur dont le visa est donné.</p>
	 * Il s'agit d'un appel asynchrone
	 * @param visa VISA de l'utilisateur pour le compte duquel cette extraction est lancée
	 * @param extractor extracteur
	 * @return clé pour récupérer le document résultant de l'extraction
	 */
	ExtractionKey postExtractionQuery(String visa, PlainExtractor extractor);

	/**
	 * Poste une demande d'exécution de l'extracteur donné pour le compte de l'utilisateur dont le visa est donné.</p>
	 * Il s'agit d'un appel asynchrone
	 * @param visa VISA de l'utilisateur pour le compte duquel cette extraction est lancée
	 * @param extractor extracteur
	 * @return clé pour récupérer le document résultant de l'extraction
	 */
	ExtractionKey postExtractionQuery(String visa, BatchableExtractor extractor);

	/**
	 * Poste une demande d'exécution de l'extracteur donné pour le compte de l'utilisateur dont le visa est donné.</p>
	 * Il s'agit d'un appel asynchrone
	 * @param visa VISA de l'utilisateur pour le compte duquel cette extraction est lancée
	 * @param extractor extracteur
	 * @return clé pour récupérer le document résultant de l'extraction
	 */
	ExtractionKey postExtractionQuery(String visa, BatchableParallelExtractor extractor);

	/**
	 * Demande l'arrêt d'un job (s'il est en cours) ou son annulation (s'il est encore en attente)
	 * @param job le job à annuler
	 */
	void cancelJob(ExtractionJob job);

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
