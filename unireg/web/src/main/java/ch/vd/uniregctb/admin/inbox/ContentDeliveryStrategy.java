package ch.vd.uniregctb.admin.inbox;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Différentes stratégies pour reconstituer le flux à télécharger
 */
public interface ContentDeliveryStrategy {

	/**
	 * @param mimeType le type MIME original
	 * @return le type MIME définitif du contenu (en cas d'encapsulation du flux initial dans autre chose, ce type n'est pas forcément celui du flux initial)
	 */
	String getMimeType(String mimeType);

	/**
	 * @return <code>true</code> si la "content-disposition" doit être "attachment", <code>false</code> s'il doit être "inline"
	 */
	boolean isAttachment();

	/**
	 * Copie du flux initial dans le flux de la réponse HTTP
	 * @param in flux initial
	 * @param out flux de la réponse HTTP
	 * @throws java.io.IOException en cas de problème à la recopie
	 */
	void copyToOutputStream(InputStream in, OutputStream out) throws IOException;
}
