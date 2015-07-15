package ch.vd.uniregctb.load;

/**
 * Interface d'interrogation des détails sur une activité en cours
 */
public interface LoadDetail {

	/**
	 * @return une description textuelle de l'activité en cours
	 */
	String getDescription();

	/**
	 * @return la durée actuelle d'exécution de l'activité en cours, en millisecondes
	 */
	long getDurationMs();

	/**
	 * @return le nom du thread sur lequel tourne l'activité en cours
	 */
	String getThreadName();
}
