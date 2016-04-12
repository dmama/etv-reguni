package ch.vd.uniregctb.migration.pm.utils;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * Classe qui va chercher dans des variables du système (cf {@link System#getProperties()}) les chemins
 * d'accès aux fichiers de propriétés (afin de pouvoir mettre ces fichier de propriétés sur la ligne de commandes)
 */
public class PreferencesPlaceholderConfigurer extends org.springframework.beans.factory.config.PreferencesPlaceholderConfigurer {

	private int numberOfConfFiles;
	private String confFileEnvironmentVarsPrefix;

	public void setNumberOfConfFiles(int numberOfConfFiles) {
		this.numberOfConfFiles = numberOfConfFiles;
	}

	public void setConfFileEnvironmentVarsPrefix(String confFileEnvironmentVarsPrefix) {
		this.confFileEnvironmentVarsPrefix = confFileEnvironmentVarsPrefix;
	}

	@Override
	public void afterPropertiesSet() {
		// remplissage de la variable "locations"
		final Resource[] resources = new Resource[numberOfConfFiles];
		for (int i = 0 ; i < numberOfConfFiles ; ++ i) {
			final String envVariableName = String.format("%s.%d", confFileEnvironmentVarsPrefix, i);
			resources[i] = new FileSystemResource(System.getProperty(envVariableName));
		}
		setLocations(resources);

		// suite du processus d'initialisation
		super.afterPropertiesSet();
	}
}
