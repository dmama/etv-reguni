package ch.vd.uniregctb.utils;

/**
 * Maintient la donnée de l'environnement d'exécution
 */
public class EnvironnementHelper {

	private static String environnement;

	public void setEnvironnement(String environnement) {
		EnvironnementHelper.environnement = environnement;
	}

	public static String getEnvironnement() {
		return EnvironnementHelper.environnement;
	}
}
