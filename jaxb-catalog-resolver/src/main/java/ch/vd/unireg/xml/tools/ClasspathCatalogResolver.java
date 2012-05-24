package ch.vd.unireg.xml.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.sun.org.apache.xerces.internal.dom.DOMInputImpl;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

/**
 * Catalog resolver qui recherche dans le classpath les XSDs dont le schemaLocation commence par http://ressources.etat-de-vaud.ch/fiscalite/registre/ ou http://www.ech.ch/xmlns. Spécifique au projet
 * Unireg.
 */
@SuppressWarnings({"UnusedDeclaration"})
public class ClasspathCatalogResolver extends com.sun.org.apache.xml.internal.resolver.tools.CatalogResolver implements LSResourceResolver {

	final static File tempDir;

	private static Map<String, String> resolvedCache = Collections.synchronizedMap(new HashMap<String, String>());

	static {
		System.out.println("Loading ClasspathCatalogResolver ...");

		// Création d'un répertoire de travail temporaire
		try {
			tempDir = File.createTempFile("ClasspathCatalogResolver", "");
			if (!tempDir.delete()) {
				throw new RuntimeException("Cannot delete file " + tempDir.getCanonicalPath());
			}
			if (!tempDir.mkdir()) {
				throw new RuntimeException("Cannot create directory " + tempDir.getCanonicalPath());
			}
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getResolvedEntity(final String publicId, final String systemId) {

		//System.out.println("Resolving publicId [" + publicId + "] - systemId [" + systemId + "]...");

		final String result = super.getResolvedEntity(publicId, systemId);
		if (result != null) {
			return result;
		}

		final String r = resolvedCache.get(systemId);
		if (r != null) {
			return r;
		}

		final String r2 = resolveInClasspath(systemId);
		if (r2 != null) {
			resolvedCache.put(systemId, r2);
			return r2;
		}

		return null;
	}

	@Override
	public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {

		final String resolvedId = getResolvedEntity(publicId, systemId);
		if (resolvedId != null) {
			return new DOMInputImpl(publicId, resolvedId, baseURI);
		}
		return null;
	}

	/**
	 * Cette méthode va résoudre le systemId fourni et retourner un chemin vers une copie local de la ressource.
	 *
	 * @param systemId le systemId, c'est-à-dire la valeur fournie dans l'attribut schemaLocation d'un xs:import (par exemple "http://www.ech.ch/xmlns/eCH-0010/4/eCH-0010-4-0f.xsd")
	 * @return un chemin vers la ressource locale (par exemple "file:/tmp/ClasspathCatalogResolver5317162594717290146/eCH-0010-4-0f.xsd")
	 */
	private String resolveInClasspath(String systemId) {

		final String resourceLookup = systemId2Classpath(systemId);

		final URL resource;
		try {
			// on essaie de résoudre le XSD dans le classpath
			resource = Thread.currentThread().getContextClassLoader().getResource(resourceLookup);
			if (resource != null) {
				final URL path = createTempFile(resourceLookup, resource);
				System.out.println("Resolved systemId [" + systemId + "] to [" + path.toString() + "]");
				return path.toString();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.err.println("Caught " + e.getClass().getSimpleName() + ": " + e.getMessage() + " exception when resolving systemId [" + systemId + "]");
		}

		return null;
	}

	/**
	 * Cette méthode permet de transformer un sytemId en un chemin relatif dans le classpath.
	 *
	 * @param systemId le systemId, c'est-à-dire la valeur fournie dans l'attribut schemaLocation d'un xs:import (par exemple "http://www.ech.ch/xmlns/eCH-0010/4/eCH-0010-4-0f.xsd")
	 * @return un chemin relatif dans le classpath (par exemple "eCH-0010-4-0f.xsd")
	 */
	private String systemId2Classpath(String systemId) {
		String resourceLookup = systemId;
		if (systemId.startsWith("http://ressources.etat-de-vaud.ch/fiscalite/registre/")) {
			// pour tout ce qui Unireg, on va chercher dans le classpath, en tenant compte du répertoire relatif
			resourceLookup = systemId.replace("http://ressources.etat-de-vaud.ch/fiscalite/registre/", "");
		}
		else if (systemId.startsWith("http://www.ech.ch/xmlns")) {
			// pour tout ce qui eCH, on va chercher dans la racine du classpath
			int pos = systemId.lastIndexOf('/');
			resourceLookup = systemId.substring(pos + 1);
		}
		return resourceLookup;
	}

	/**
	 * Cette méthode prend le fichier donné en paramètre (par exemple "file:jar:blabla.jar!ech/ech0010-4-0.xsd") et en fait une copie temporaire sur le filesystem (par exemple
	 * "file:/tmp/ClasspathCatalogResolver8897806772197090291/ech0010-4-0.xsd"). Ceci permet de contourner un problème d'URI invalide (= le point d'exclamation n'est pas supporté) qui apparaît plus loin
	 * dans le parsing des XSDs.
	 *
	 * @param filename le nom du fichier (exemple : "ech0010-4-0.xsd")
	 * @param filepath l'URL vers le fichier (exemple : "file:jar:blabla.jar!ech/ech0010-4-0.xsd")
	 * @return une URL vers la copie temporaire du fichier d'entrée
	 * @throws java.io.IOException en cas d'impossiblité de créer le fichier temporaire
	 */
	private synchronized URL createTempFile(String filename, URL filepath) throws IOException {

		// On gère le cas des sous-répertoires dans le nom de fichier
		File parent = tempDir;
		int sep = filename.lastIndexOf(File.separatorChar);
		if (sep >= 0) {
			parent = new File(tempDir, filename.substring(0, sep));
			filename = filename.substring(sep + 1);
			if (!parent.exists() && !parent.mkdirs()) {
				throw new RuntimeException("Impossible de créer le répertoire " + parent.getCanonicalPath());
			}
		}

		//System.out.println("Creating file  [" + parent.getCanonicalPath() + "] / [" + filename + "]...");

		// Création du fichier temporaire
		final File f = new File(parent, filename);
		if (f.exists()) {
			throw new RuntimeException("Programming error : file [" + f.getCanonicalPath() + "] already exists !");
		}
		f.deleteOnExit();

		// Copie des données
		final FileOutputStream out = new FileOutputStream(f);
		final InputStream in = filepath.openStream();
		try {
			copy(in, out);
		}
		finally {
			out.close();
			in.close();
		}

		return f.toURI().toURL();
	}

	private static final int IO_BUFFER_SIZE = 4 * 1024;

	private static void copy(InputStream in, OutputStream out) throws IOException {
		byte[] b = new byte[IO_BUFFER_SIZE];
		int read;
		while ((read = in.read(b)) != -1) {
			out.write(b, 0, read);
		}
	}
}
