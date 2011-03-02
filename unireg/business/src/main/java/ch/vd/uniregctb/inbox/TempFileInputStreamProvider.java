package ch.vd.uniregctb.inbox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Permet de fournir des flux de lecture sur le contenu d'un fichier temporaire (ce fichier sera détruit
 * au moment du {@link #close()})
 */
public final class TempFileInputStreamProvider {

	private final File tempFile;

	/**
	 * Constructeur. L'intégralité du flux passé en paramètre est consommé à la sortie de cet appel, mais
	 * le flux n'est pas fermé.
	 * @param prefix préfixe à utiliser dans la génération du nom du fichier temporaire
	 * @param src flux d'entrée dont le contenu sera recopié dans un fichier temporaire sur le disque
	 * @throws IOException en cas de problème
	 */
	public TempFileInputStreamProvider(String prefix, InputStream src) throws IOException {
		tempFile = File.createTempFile(prefix, ".tmp");
		tempFile.deleteOnExit();
		copyStreamToFile(src, tempFile);
	}

	private static void copyStreamToFile(InputStream in, File dest) throws IOException {
		final byte[] buffer = new byte[16384];      // 16K

		final FileOutputStream fos = new FileOutputStream(dest);
		try {
			int len;
			do {
				len = in.read(buffer);
				if (len > 0) {
					fos.write(buffer, 0, len);
				}
			}
			while (len > 0);
		}
		finally {
			fos.close();
		}
	}

	public InputStream getInputStream() throws IOException {
		return new FileInputStream(tempFile);
	}

	@SuppressWarnings({"ResultOfMethodCallIgnored"})
	public void close() {
		tempFile.delete();
	}
}
