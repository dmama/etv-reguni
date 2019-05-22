package ch.vd.unireg.interfaces.entreprise.rcent;

import javax.xml.transform.Source;
import java.io.IOException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import ch.vd.unireg.common.XmlUtils;

/**
 * Classe utilitaire aidant au chargement du schema des XMLs de RCEnt.
 */
public class RCEntSchemaHelper {

	/*
		Configuration des schémas applicables pour le décodage des données RCEnt
    */
	public static final String[] RCENT_SCHEMA = new String[]{
			"eCH-0010-6-0.xsd",
			"eCH-0007-6-0.xsd",
			"eCH-0008-3-0.xsd",
			"eCH-0044-4-1.xsd",
			"eCH-0046-3-0.xsd",
			"eCH-0097-2-0.xsd",
			"eCH-0098-3-0.xsd",
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
		return XmlUtils.toSourcesArray(RCENT_SCHEMA);
	}

}
