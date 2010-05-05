package ch.vd.uniregctb.utils;

/**
 * Bean qui expose les différences modes de fonctionnement d'Unireg (configurés de manière externe ou à la compilation).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class UniregModeHelper {

	private static boolean testMode = false;
	private static boolean standalone = false;
	private static String environnement;

	/**
	 * @return <i>vrai</i> si le mode testing est activé; <i>faux</i> autrement.
	 */
	public static boolean isTestMode() {
		return testMode;
	}

	/**
	 * @return <i>vrai</i> si Unireg est compilé en mode standalone (= host-interface et l'esb mockés); <i>faux</i> autrement.
	 */
	public static boolean isStandalone() {
		return standalone;
	}

	public static String getEnvironnement() {
		return environnement;
	}

	public void setTestMode(String v) {
		testMode = ("true".equalsIgnoreCase(v) || "1".equals(v) || "yes".equalsIgnoreCase(v));
	}

	public void setStandalone(boolean v) {
		standalone = v;
	}

	public void setEnvironnement(String environnement) {
		UniregModeHelper.environnement = environnement;
	}
}
