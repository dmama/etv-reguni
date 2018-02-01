package ch.vd.unireg.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class TypedDataContainer implements AutoCloseable {

	private final String mimeType;
	private final TempFileInputStreamProvider contentProvider;
	private final String filenameRadical;

	/**
	 * @param mimeType type MIME du contenu
	 * @param content contenu de l'attachement (le flux sera consommé directement et fermé)
	 * @param tempFilePrefix préfixe interne du nom du fichier temporaire utilisé pour le stockage
	 * @param filenameRadical radical du nom de fichier type pour cette donnée (i.e. sans l'extension, qui pourra être construite plus tard d'après le mime-type)
	 * @throws IOException en cas de problème avec le flux
	 */
	protected TypedDataContainer(String mimeType, InputStream content, String tempFilePrefix, String filenameRadical) throws IOException {
		this.mimeType = mimeType;
		this.contentProvider = new TempFileInputStreamProvider(tempFilePrefix, content);
		this.filenameRadical = filenameRadical;
	}

	/**
	 * @param mimeType type MIME du contenu
	 * @param content contenu de l'attachement
	 * @param tempFilePrefix préfixe interne du nom du fichier temporaire utilisé pour le stockage
	 * @param filenameRadical radical du nom de fichier type pour cette donnée (i.e. sans l'extension, qui pourra être construite plus tard d'après le mime-type)
	 * @throws IOException en cas de problème avec le flux
	 */
	protected TypedDataContainer(String mimeType, byte[] content, String tempFilePrefix, String filenameRadical) throws IOException {
		this(mimeType, new ByteArrayInputStream(content), tempFilePrefix, filenameRadical);
	}

	public String getMimeType() {
		return mimeType;
	}

	public InputStream getContent() throws IOException {
		return contentProvider.getInputStream();
	}

	public String getFilenameRadical() {
		return filenameRadical;
	}

	public long getSize() {
		return contentProvider.getFileSize();
	}

	/**
	 * Appelé lorsque l'élément est envoyé aux oubliettes (pour d'éventuels nettoyages)
	 */
	public void close() {
		contentProvider.close();
	}

}
