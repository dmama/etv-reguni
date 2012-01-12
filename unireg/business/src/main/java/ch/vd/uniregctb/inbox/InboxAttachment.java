package ch.vd.uniregctb.inbox;

import java.io.IOException;
import java.io.InputStream;

/**
 * Attachement téléchargeable
 */
public class InboxAttachment {

	private final String mimeType;
	private final TempFileInputStreamProvider contentProvider;
	private final String filenameRadical;

	/**
	 * Attachement à un élément de la boîte de réception
	 *
	 * @param mimeType type MIME du contenu
	 * @param content contenu de l'attachement (le flux sera consommé directement et fermé)
	 * @param filenameRadical radical du nom de fichier type pour cet attachement (i.e. sans l'extension, qui pourra être construite plus tard d'après le mime-type)
	 * @throws IOException en cas de problème avec le flux
	 */
	public InboxAttachment(String mimeType, InputStream content, String filenameRadical) throws IOException {
		this.mimeType = mimeType;
		this.contentProvider = new TempFileInputStreamProvider("ur-inbox-elt-", content);
		this.filenameRadical = filenameRadical;
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
	public void onDiscard() {
		contentProvider.close();
	}
}
