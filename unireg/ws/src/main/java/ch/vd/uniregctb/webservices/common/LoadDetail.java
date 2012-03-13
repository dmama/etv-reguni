package ch.vd.uniregctb.webservices.common;

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
}
