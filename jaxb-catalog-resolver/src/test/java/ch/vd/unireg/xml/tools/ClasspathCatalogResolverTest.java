package ch.vd.unireg.xml.tools;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;

import org.junit.Test;

public class ClasspathCatalogResolverTest {

	/**
	 * Ce test vérifie que la méthode createTempFile extrait bien un fichier d'un JAR et le crée bien dans un répertoire temporaire.
	 */
	@Test
	public void testCreateTempFile() throws Exception {

		final URL jar = Thread.currentThread().getContextClassLoader().getResource("jaxb2-specimen.jar");
		assertNotNull(jar);

		final String filename = "infra/unireg-taxoffices-1.xsd";
		final URL filepath = new URL("jar", "", -1, jar.toString() + "!/infra/unireg-taxoffices-1.xsd");

		// on extrait le fichier et on le stock dans un répertorie temporaire
		ClasspathCatalogResolver resolver = new ClasspathCatalogResolver();
		final URL tempFile = resolver.createTempFile(filename, filepath);
		assertNotNull(tempFile);

		// le fichier doit exister sur le disque
		assertFalse(tempFile.toString().contains("!"));
		final File file = new File(tempFile.toURI());
		assertTrue(file.exists());
	}
}