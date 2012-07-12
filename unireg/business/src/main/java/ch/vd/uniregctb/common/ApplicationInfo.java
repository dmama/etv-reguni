package ch.vd.uniregctb.common;

/**
 * Expose de manière statique des informations (version, build, ...) sur l'application.
 */
public class ApplicationInfo {

	private static String name;
	private static String version;
	private static String description;

	/**
	 * @return le nom de l'application
	 */
	public static String getName() {
		return name;
	}

	public void setName(String name) {
		ApplicationInfo.name = name;
	}

	/**
	 * @return la version complète de l'application
	 */
	public static String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		ApplicationInfo.version = version;
	}

	/**
	 * @return la description de l'application
	 */
	public static String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		ApplicationInfo.description = description;
	}
}
