package ch.vd.unireg.xml.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Catalog resolver qui recherche dans le classpath les XSDs dont le schemaLocation commence par http://ressources.etat-de-vaud.ch/fiscalite/registre/ ou http://www.ech.ch/xmlns. Spécifique au projet
 * Unireg.
 */
@SuppressWarnings({"UnusedDeclaration"})
public class ClasspathCatalogResolver extends com.sun.org.apache.xml.internal.resolver.tools.CatalogResolver {

	@Override
	public String getResolvedEntity(final String publicId, final String systemId) {

		//System.out.println("Resolving publicId [" + publicId + "] - systemId [" + systemId + "]...");

		final String result = super.getResolvedEntity(publicId, systemId);
		if (result != null) {
			return result;
		}

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

		final URL resource;
		try {
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

	final static File d;

	static {
		System.out.println("Loading ClasspathCatalogResolver ...");

		// Création d'un répertoire temporaire
		try {
			d = File.createTempFile("ClasspathCatalogResolver", "");
			if (!d.delete()) {
				throw new RuntimeException("Impossible de détruire le fichier " + d.getCanonicalPath());
			}
			if (!d.mkdir()) {
				throw new RuntimeException("Impossible de créer le répertoire " + d.getCanonicalPath());
			}
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private static Map<URL, URL> tempFilesMap = new HashMap<URL, URL>();

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

		URL t = tempFilesMap.get(filepath);

		if (t == null) {

			// On gère le cas des sous-répertoires dans le nom de fichier
			File parent = d;
			int sep = filename.lastIndexOf(File.separatorChar);
			if (sep >= 0) {
				parent = new File(d, filename.substring(0, sep));
				filename = filename.substring(sep + 1);
				if (!parent.mkdirs()) {
					throw new RuntimeException("Impossible de créer le répertoire " + parent.getCanonicalPath());
				}
			}

			//System.out.println("Creating file  [" + parent.getCanonicalPath() + "] / [" + filename + "]...");

			// Création du fichier temporaire
			final File f = new File(parent, filename);
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

			t = f.toURI().toURL();
			tempFilesMap.put(filepath, t);
		}

		return t;
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
