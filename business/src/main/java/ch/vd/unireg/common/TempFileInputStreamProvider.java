package ch.vd.unireg.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

/**
 * Permet de fournir des flux de lecture sur le contenu d'un fichier temporaire (ce fichier sera détruit
 * au moment du {@link #close()})
 */
public final class TempFileInputStreamProvider implements AutoCloseable {

	private final TemporaryFile file;
	private final long size;

	/**
	 * Constructeur. L'intégralité du flux passé en paramètre est consommé à la sortie de cet appel et le flux est fermé
	 * @param prefix préfixe à utiliser dans la génération du nom du fichier temporaire
	 * @param src flux d'entrée dont le contenu sera recopié dans un fichier temporaire sur le disque
	 * @throws IOException en cas de problème
	 */
	public TempFileInputStreamProvider(String prefix, InputStream src) throws IOException {
		file = new TemporaryFile(prefix);
		try (OutputStream out = file.openOutputStream()) {
			IOUtils.copy(src, out);
		}
		catch (RuntimeException | Error | IOException e) {
			file.close();
			throw e;
		}

		size = file.getFullPath().length();
	}

	public InputStream getInputStream() throws IOException {
		return file.openInputStream();
	}

	public long getFileSize() {
		return size;
	}

	public void close() {
		file.close();
	}
}
