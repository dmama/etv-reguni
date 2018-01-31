package ch.vd.uniregctb.inbox;

import java.io.IOException;
import java.io.InputStream;

import ch.vd.uniregctb.common.TypedDataContainer;

/**
 * Attachement téléchargeable
 */
public class InboxAttachment extends TypedDataContainer {

	/**
	 * Attachement à un élément de la boîte de réception
	 *
	 * @param mimeType type MIME du contenu
	 * @param content contenu de l'attachement (le flux sera consommé directement et fermé)
	 * @param filenameRadical radical du nom de fichier type pour cet attachement (i.e. sans l'extension, qui pourra être construite plus tard d'après le mime-type)
	 * @throws IOException en cas de problème avec le flux
	 */
	public InboxAttachment(String mimeType, InputStream content, String filenameRadical) throws IOException {
		super(mimeType, content, "ur-inbox-elt-", filenameRadical);
	}
}
