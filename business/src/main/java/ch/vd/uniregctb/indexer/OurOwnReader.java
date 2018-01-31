package ch.vd.uniregctb.indexer;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.BitSet;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Le but de ce reader est de transformer la chaîne de caractères lue en entrée
 * en une autre en tenant compte des contraintes suivantes :
 * <ul>
 *     <li>en absence de point ".", ne change rien à ce que renvoie le {@link Reader} encapsulé</li>
 *     <li>ne considère comme particuliers que les points non-suivis d'un espace</li>
 *     <li>si un tel point est trouvé
 *       <ul>
 *          <li>s'il n'y a qu'une lettre de part et d'autre, on enlève le point : les deux lettres forment alors un mots</li>
 *          <li>s'il y a plus d'une lettre (ou autre chose) d'un côté ou de l'autre, le point est remplacé par un espace</li>
 *       </ul>
 *     </li>
 * </ul>
 * <br/>
 * <b>Note :</b> Cette classe n'est pas <i>thread-safe</i> !!
 */
public class OurOwnReader extends Reader {

	private final Reader target;
	private final char[] buffer = new char[8 * 1024];      // 8K, devrait être largement suffisant, non ?
	private final BitSet dotsToErase = new BitSet(buffer.length);

	public OurOwnReader(Reader target) {
		this.target = target;
	}

	@Override
	public int read(@NotNull char[] cbuf, int off, int len) throws IOException {
		// ne fonctionne pas si mon buffer local est trop petit...
		if (len > buffer.length) {
			throw new IllegalStateException("Taille du buffer trop petite (" + buffer.length + ") là où " + len + " semble nécessaire");
		}

		// on va commencer par transferer le nécessaire dans notre buffer local
		final int read = target.read(buffer, 0, len);
		if (read >= buffer.length) {
			// wow, c'est super long... il faudrait modifier la taille !!!
			throw new IllegalStateException("Taille du buffer trop petite : " + buffer.length);
		}

		// y avait-il quelque chose dans le reader target que nous venons de lire ?
		int finalSize = read;
		if (read > 0) {

			// retrouvons les points sans espace ensuite
			dotsToErase.clear();

			// pas la peine de regarder si le dernier caractère est un point puisqu'il ne sera de toute façon pas suivi d'une lettre...
			for (int index = 0 ; index < read - 1 ; ++ index) {
				if (buffer[index] == '.' && !Character.isWhitespace(buffer[index + 1])) {
					final int lettersBefore = index > 0 ? countLetters(index - 1, -1) : 0;
					final int lettersAfter = countLetters(index + 1, 1);
					if (lettersAfter == 1 && lettersBefore == 1) {
						dotsToErase.set(index);
					}
					else if (lettersAfter > 0 || lettersBefore > 0) {
						// séparation de mot -> transformation en espace
						buffer[index] = ' ';
					}
				}
			}

			// il faut maintenant effacer les points marqués
			for (int index = dotsToErase.length() ; (index = dotsToErase.previousSetBit(index - 1)) >= 0 ; ) {
				System.arraycopy(buffer, index + 1, buffer, index, buffer.length - index - 1);
				-- finalSize;
			}

			// copie dans le buffer de sortie
			System.arraycopy(buffer, 0, cbuf, off, finalSize);
		}

		return finalSize;
	}

	private int countLetters(int startIndex, int step) {
		int count = 0;
		int index = startIndex;
		while (index >= 0 && index < buffer.length && Character.isAlphabetic(buffer[index])) {
			++ count;
			index += step;
		}
		return count;
	}

	@Override
	public void close() throws IOException {
		target.close();
	}

	/**
	 * Méthode utilitaire de conversion d'une chaîne de caractère en une autre, qui utilise le filtrage mis en place dans ce Reader
	 * @param src chaîne de caractères source
	 * @return chaîne de caractères transformée
	 */
	public static String convert(String src) {
		if (src == null) {
			return null;
		}

		try {
			final OurOwnReader ownReader = new OurOwnReader(new StringReader(src));
			final StringWriter writer = new StringWriter(src.length());
			IOUtils.copy(ownReader, writer);
			return writer.toString();
		}
		catch (IOException e) {
			// ne devrait pas arriver, nous ne manipulons que des chaînes de caractères en mémoire...
			throw new RuntimeException(e);
		}
	}
}
