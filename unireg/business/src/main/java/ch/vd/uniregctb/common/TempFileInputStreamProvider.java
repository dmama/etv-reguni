package ch.vd.uniregctb.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * Permet de fournir des flux de lecture sur le contenu d'un fichier temporaire (ce fichier sera détruit
 * au moment du {@link #close()})
 */
public final class TempFileInputStreamProvider {

	private final File tempFile;
	private final long size;

	/**
	 * Constructeur. L'intégralité du flux passé en paramètre est consommé à la sortie de cet appel et le flux est fermé
	 * @param prefix préfixe à utiliser dans la génération du nom du fichier temporaire
	 * @param src flux d'entrée dont le contenu sera recopié dans un fichier temporaire sur le disque
	 * @throws IOException en cas de problème
	 */
	public TempFileInputStreamProvider(String prefix, InputStream src) throws IOException {
		tempFile = File.createTempFile(prefix, ".tmp");
		tempFile.deleteOnExit();
		try {
			copyStreamToFile(src, tempFile);
		}
		finally {
			src.close();
		}
		size = tempFile.length();
	}

	private static void copyStreamToFile(InputStream in, File dest) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(dest)) {
			IOUtils.copy(in, fos);
		}
	}

	public InputStream getInputStream() throws IOException {
		return new FileInputStream(tempFile);
	}

	public long getFileSize() {
		return size;
	}

	public void close() {
		//noinspection ResultOfMethodCallIgnored
		tempFile.delete();
	}
}
