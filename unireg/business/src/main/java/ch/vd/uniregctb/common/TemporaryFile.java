package ch.vd.uniregctb.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jetbrains.annotations.NotNull;

/**
 * Un container de données temporaires sur disque
 */
public class TemporaryFile implements AutoCloseable {

	private final File fileLocation;

	public TemporaryFile(@NotNull String prefix) throws IOException {
		this.fileLocation = File.createTempFile(prefix, ".tmp");
		this.fileLocation.deleteOnExit();       // normally deleted before that...
	}

	/**
	 * @return un {@link java.io.OutputStream} pour écrire dans la zone temporaire (ne pas oublier de fermer le flux!)
	 * @throws IOException en cas de problème
	 */
	public OutputStream openOutputStream() throws IOException {
		return new FileOutputStream(fileLocation);
	}

	/**
	 * @return un {@link java.io.InputStream} pour lire depuis la zone temporaire (ne pas oublier de fermer le flux!)
	 * @throws IOException en cas de problème
	 */
	public InputStream openInputStream() throws IOException {
		return new FileInputStream(fileLocation);
	}

	/**
	 * @return le chemin d'accès complet au fichier temporaire
	 * @throws IOException en cas de problème
	 */
	public File getFullPath() throws IOException {
		return fileLocation.getCanonicalFile();
	}

	@Override
	public void close() {
		//noinspection ResultOfMethodCallIgnored
		fileLocation.delete();
	}
}
