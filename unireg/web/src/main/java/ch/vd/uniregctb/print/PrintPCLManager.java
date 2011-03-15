package ch.vd.uniregctb.print;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface PrintPCLManager {


	/**
	 * Ouvre un flux PCL et le place dans la réponse HTTP fournie
	 *
	 * @param response réponse HTTP à utiliser pour y placer le flux PCL
	 * @param pcl données du flux PCL
	 * @throws IOException en cas de problème IO
	 */
	void openPclStream(HttpServletResponse response, byte[] pcl) throws IOException;

	/**
	 * @return si oui ou non le contenu doit être un attachement (voir Content-disposition HTTP header)
	 * ou inline dans la réponse HTTP
	 */
	boolean isAttachmentContent();

	/**
	 * @return le type MIME effectif du contenu placé dans la réponse HTTP
	 */
	String getActualMimeType();

	/**
	 * Recopie le contenu du flux d'entrée (format PCL) dans le flux de sortie
	 * @param in flux au format PCL
	 * @param out flux de la réponse HTTP
	 * @throws java.io.IOException en cas de problème à la recopie
	 */
	void copyToOutputStream(InputStream in, OutputStream out) throws IOException;
}
