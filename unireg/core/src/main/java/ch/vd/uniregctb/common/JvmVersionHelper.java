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
	 * Vérifie que la JVM est compatible avec un Host-Interfaces
	 */
	public static void checkJvmWrtHostInterfaces() {
		checkJavaVersion(Version.JAVA_1_6);
		checkArraySerializationWorkaround();
	}

	/**
	 * Vérifie que le workaround de sérialisation des tableaux entre java 1.5 et java 1.6 est activé.
	 */
	public static void checkArraySerializationWorkaround() {
		final String enabled = System.getProperty("sun.lang.ClassLoader.allowArraySyntax");
		if (!enabled.equals("true")) {
			throw new RuntimeException("La proriété système 'sun.lang.ClassLoader.allowArraySyntax' doit être renseignée à vrai sur les JVM 1.6 pour permettre une communication avec Host-Interfaces.");
		}
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
