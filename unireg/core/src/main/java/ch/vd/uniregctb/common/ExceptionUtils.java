package ch.vd.uniregctb.common;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

/**
 * Classe utilitaire pour la manipulation des exceptions.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ExceptionUtils {

	/**
	 * Extrait la call-stack de l'exception spécifiée sous forme de string
	 *
	 * @param exception
	 *            l'exception dont on veut extraire la call-stack
	 * @return une string contenant la call-stack de l'exception.
	 */
	public static String extractCallStack(Exception exception) {
		PrintWriter s = null;
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			s = new PrintWriter(os);
			exception.printStackTrace(s);
			s.flush();
			return os.toString();
		}
		finally {
			if (s != null) {
				s.close();
			}
		}
	}
}
