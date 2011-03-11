package ch.vd.uniregctb.extraction;

import java.util.Date;

/**
 * Interface implémentée par la structure interne au service d'extractions asynchrones
 * qui représente une extraction à effectuer
 */
public interface ExtractionJob {

	/**
	 * @return le résultat de la procédure d'extraction asynchrone
	 */
	ExtractionResult getResult();

	/**
	 * @return un identifiant unique pour le job d'extraction
	 */
	ExtractionKey getKey();

	/**
	 * @return instant de création du job (= début de l'attente de slot disponible, pas démarrage effectif)
	 */
	Date getCreationDate();

	/**
	 * @return <code>true</code> si le job est actuellement en cours d'exécution
	 */
	boolean isRunning();

	/**
	 * @return la durée d'exécution du job en millisecondes depuis son démarrage jusqu'à sa fin (ou, s'il n'est pas terminé, jusqu'à maintenant) ; <code>null</code> si le job n'est pas commencé
	 */
	Long getDuration();

	/**
	 * @return le message d'avancement courant du job
	 */
	String getRunningMessage();

	/**
	 * @return l'avancement, en pourcentage (0-100) du job d'extraction, si celui-ci est disponible (opértation optionnelle)
	 */
	Integer getPercentProgression();

	/**
	 * @return une description textuelle de l'extraction
	 */
	String getDescription();

}
