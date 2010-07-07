package ch.vd.uniregctb.common;

/**
 * Permet de vérifier que la JVM a bien la bonne version
 */
public class JvmVersionHelper {

	public static enum Version {
		JAVA_1_5("1.5.", "1.5"),
		JAVA_1_6("1.6.", "1.6");

		private final String prefixe;
		private final String displayName;

		private Version(String prefixe, String displayName) {
			this.prefixe = prefixe;
			this.displayName = displayName;
		}
	}

	/**
	 * Renvoie une RuntimeException si la JVM n'est pas en version 1.5
	 */
	public static void checkJava_1_5() {
		checkJavaVersion(Version.JAVA_1_5);
	}

	/**
	 * Renvoie une RuntimeException si la JVM n'est pas compatible avec la version donnée
	 */
	public static void checkJavaVersion(Version versionAttendue) {

		// la version de la JVM doit être compatible avec celle attendue
		final String version = System.getProperty("java.version");
		if (version == null || version.trim().length() == 0) {
			throw new RuntimeException("Impossible de connaître le numéro de version de la JVM (java.version est vide)");
		}
		else if (!version.startsWith(versionAttendue.prefixe)) {
			throw new RuntimeException(String.format("Une version %s de la JVM est nécessaire (version trouvée : %s)", versionAttendue.displayName, version));
		}
	}
}
