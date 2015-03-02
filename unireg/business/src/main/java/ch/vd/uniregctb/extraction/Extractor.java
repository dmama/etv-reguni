package ch.vd.uniregctb.extraction;

import ch.vd.shared.batchtemplate.StatusManager;

/**
 * Extracteur de données sous la forme d'un fichier CSV par exemple
 */
public interface Extractor {

	/**
	 * Demande d'interruption de l'extraction (en général sur un autre
	 * thread que celui qui est en cours d'exécution)
	 */
	void interrupt();

	/**
	 * @return <code>true</code> si la méthode {@link #interrupt()} a été appelée
	 */
	boolean wasInterrupted();

	/**
	 * @return Message fourni par l'extracteur pendant son exécution pour indiquer (optionnel) ce qu'il est en train de faire
	 */
	String getRunningMessage();

	/**
	 * @return Indicateur (optionnel) de pourcentage d'avancement (0-100)
	 */
	Integer getPercentProgression();

	/**
	 * @return StatusManager lié à cet extracteur
	 */
	StatusManager getStatusManager();

	/**
	 * @return Un nom qui décrit le type d'extraction
	 */
	String getExtractionName();

	/**
	 * @return Une description textuelle (présentable dans une IHM) de l'extraction demandée
	 */
	String getExtractionDescription();
}
