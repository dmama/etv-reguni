package ch.vd.uniregctb.inbox;

import java.io.IOException;
import java.io.InputStream;

/**
 * Attachement téléchargeable
 */
public class InboxAttachment {

	private final String mimeType;
	private final TempFileInputStreamProvider contentProvider;
	private final String filename;

	/**
	 * Attachement à un élément de la boîte de réception
	 *
	 * @param mimeType type MIME du contenu
	 * @param content contenu de l'attachement (le flux sera consommé directement et fermé)
	 * @param filename nom de fichier type pour cet attachement
	 * @throws IOException en cas de problème avec le flux
	 */
	public InboxAttachment(String mimeType, InputStream content, String filename) throws IOException {
		this.mimeType = mimeType;
		this.contentProvider = new TempFileInputStreamProvider("ur-inbox-elt-", content);
		this.filename = filename;
	}

	public String getMimeType() {
		return mimeType;
	}

	public InputStream getContent() throws IOException {
		return contentProvider.getInputStream();
	}

	public String getFilename() {
		return filename;
	}

	/**
	 * Appelé lorsque l'élément est envoyé aux oubliettes (pour la clôture du flux d'entrée
	 * et d'éventuels nettoyages)
	 */
	public void onDiscard() {
		contentProvider.close();
	}
}
