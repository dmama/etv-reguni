package ch.vd.unireg.stats;

import java.util.Date;

/**
 * Interface utilisée pour l'affichage, à intervale régulier, de l'avancement des batchs dans les logs
 */
public interface JobMonitor {

	/**
	 * @return la date et l'heure de démarrage du batch, <code>null</code> si le batch n'est pas en cours d'exécution
	 */
	Date getStartDate();

	/**
	 * @return pourcentage de complétion du batch (<code>null</code> si inconnu)
	 */
	Integer getPercentProgression();

	/**
	 * @return description textuelle de l'avancement du batch
	 */
	String getRunningMessage();
}
