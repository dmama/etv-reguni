package ch.vd.unireg.stats;

import java.time.Duration;

/**
 * Interface d'interrogation des détails sur une activité en cours
 */
public interface LoadDetail {

	/**
	 * @return une description textuelle de l'activité en cours
	 */
	String getDescription();

	/**
	 * @return la durée actuelle d'exécution de l'activité en cours
	 */
	Duration getDuration();

	/**
	 * @return le nom du thread sur lequel tourne l'activité en cours
	 */
	String getThreadName();
}
