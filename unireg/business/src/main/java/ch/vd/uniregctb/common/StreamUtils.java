package ch.vd.uniregctb.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Classe de méthodes utilitaires pour la gestion des flux IO
 */
public abstract class StreamUtils {

	/**
	 * Recopie le contenu du stream d'entrée dans le stream de sortie (taille du buffer intermédiaire : 16K)
	 * @param in stream d'entrée
	 * @param out stream de sortie
	 * @throws IOException en cas de problème IO
	 */
	public static void copy(InputStream in, OutputStream out) throws IOException {
		copy(in, out, 16384);
	}

	/**
	 * Recopie le contenu du stream d'entrée dans le stream de sortie
	 * @param in stream d'entrée
	 * @param out stream de sortie
	 * @param bufferSize taille du tampon intermédiaire
	 * @throws IOException en cas de problème IO
	 * @throws IllegalArgumentException si la taille du buffer est négative ou nulle
	 */
	public static void copy(InputStream in, OutputStream out, int bufferSize) throws IOException {
		if (bufferSize <= 0) {
			throw new IllegalArgumentException("BufferSize should be strictly positive : " + bufferSize);
		}
		final byte[] buffer = new byte[bufferSize];
		int len;
		do {
			len = in.read(buffer);
			if (len > 0) {
				out.write(buffer, 0, len);
			}
		}
		while (len > 0);
	}
}
