package ch.vd.uniregctb.migration.pm.utils;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * Classe qui va chercher dans des variables du système (cf {@link System#getProperties()}) les chemins
 * d'accès aux fichiers de propriétés (afin de pouvoir mettre ce fichier de propriétés sur la ligne de commandes)
 */
public class PreferencesPlaceholderConfigurer extends org.springframework.beans.factory.config.PreferencesPlaceholderConfigurer {

	public void setLocationFileProps(String... propNames) {
		final Resource[] resources = new Resource[propNames.length];
		for (int i = 0 ; i < propNames.length ; ++ i) {
			resources[i] = new FileSystemResource(System.getProperty(propNames[i]));
		}
		setLocations(resources);
	}
}
