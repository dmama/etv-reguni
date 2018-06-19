package ch.vd.unireg.interfaces.organisation.rcent;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Classe utilitaire aidant au chargement du schema xml du service entreprise RCEnt.
 */
public class RCEntSchemaHelper {

	/*
		Configuration des schémas applicables pour le décodage des données RCEnt
    */
	public static final String[] RCENT_SCHEMA = new String[]{
			"eVD-0004-3-0.xsd",
			"eVD-0022-3-5.xsd",
			"eVD-0023-3-5.xsd",
			"eVD-0024-3-5.xsd"
	};

	public static Resource[] getRCEntSchemaClassPathResource() {
		Resource[] ar = new Resource[RCEntSchemaHelper.RCENT_SCHEMA.length];
		for (int i = 0; i < RCEntSchemaHelper.RCENT_SCHEMA.length; i++) {
			ar[i] = new ClassPathResource(RCEntSchemaHelper.RCENT_SCHEMA[i]);
		}
		return ar;
	}

	public static Source[] getRCEntClasspathSources() throws IOException {
		final Source[] sources = new Source[RCENT_SCHEMA.length];
		for (int i = 0, pathLength = RCENT_SCHEMA.length; i < pathLength; i++) {
			final String path = RCENT_SCHEMA[i];
			sources[i] = new StreamSource(new ClassPathResource(path).getURL().toExternalForm());
		}
		return sources;
	}

}
