package ch.vd.uniregctb.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * Classe utilitaire qui est utilisée pour générer des fichiers CSV
 */
public abstract class CsvHelper {

	public static final char COMMA = ';';
	public static final String CR = "\n";
	public static final char DOUBLE_QUOTE = '"';
	public static final String EMPTY = StringUtils.EMPTY;
	public static final String CHARSET = "ISO-8859-15";

	/**
	 * Interface implémentée par le code spécifique au remplissage d'un fichier CSV
	 * @param <T>
	 */
	public static interface Filler<T> {
		/**
		 * Remplissage de la ligne d'entête (sans le CR final)
		 * @param b destination du remplissage
		 */
		void fillHeader(StringBuilder b);

		/**
		 * Remplissage de chacune des lignes (sans le CR final)
		 * @param b destination du remplissage
		 * @param elt source de l'information à utiliser pour le remplissage
		 */
		void fillLine(StringBuilder b, T elt);
	}

	/**
	 * Méthode de remplissage de fichier Csv utilisant un {@link Filler}
	 */
	public static <T> String asCsvFile(List<T> list, String fileName, StatusManager status, int avgLineLength, Filler<T> filler) {
		String contenu = null;
		final int size = list.size();
		if (size > 0) {
			final StringBuilder b = new StringBuilder((avgLineLength + CR.length()) * size);
			filler.fillHeader(b);
			b.append(CR);

			final String message = String.format("Génération du fichier %s", fileName);
			if (status != null) {
				status.setMessage(message, 0);
			}

			final GentilIterator<T> iter = new GentilIterator<T>(list);
			while (iter.hasNext()) {
				final T info = iter.next();
				if (iter.isAtNewPercent() && status != null) {
					status.setMessage(message, iter.getPercent());
				}

				final int sizeBefore = b.length();
				filler.fillLine(b, info);
				if (sizeBefore < b.length()) {
					b.append(CR);
				}
			}

			contenu = b.toString();
		}
		return contenu;
	}

	/**
	 * Transforme la ligne spécifiée (qui peut contenir des retours de lignes embeddés) en une chaîne de caractère capable de tenir dans un champ d'un fichier CSV. Les retours de lignes sont préservés,
	 * mais les éventuels caractères interdits (" et ;) sont supprimés.
	 * @param lignes chaîne de caractères potentiellement sur plusieurs lignes
	 */
	public static String asCsvField(String lignes) {
		return asCsvField(lignes.split(CR));
	}

	/**
	 * Transforme les lignes spécifiées (un élément par ligne) en une chaîne de caractère capable de tenir dans un champ d'un fichier CSV. Les retours de lignes sont ajoutés entre chaque élément
	 * mais les éventuels caractères interdits (" et ;) sont supprimés.
	 * @param lignes chaîne de caractères potentiellement sur plusieurs lignes
	 */
	public static String asCsvField(List<String> lignes) {
		return asCsvField(lignes.toArray(new String[lignes.size()]));
	}

	/**
	 * Supression des caractères " et ;
	 * @param ligne
	 * @return
	 */
	public static String escapeChars(String ligne) {
		return StringUtils.isBlank(ligne) ? EMPTY : ligne.trim().replaceAll("[\";]", EMPTY);
	}

	/**
	 * Transforme les lignes spécifiées en une chaîne de caractère capable de tenir dans un champ d'un fichier CSV. Les retours de lignes sont préservés, mais les éventuels caractères interdits (" et ;)
	 * sont supprimés.
	 */
	public static String asCsvField(String[] lignes) {
		final StringBuilder b = new StringBuilder();
		b.append(DOUBLE_QUOTE);
		final int length = lignes.length;

		// compte les lignes non-vides
		int nbLignesNonVides = 0;
		for (int i = 0 ; i < length ; ++ i) {
			if (!StringUtils.isBlank(lignes[i])) {
				++ nbLignesNonVides;
			}
		}

		// construit la chaîne de caractères
		for (int i = 0; i < length; ++i) {
			final String ligne = lignes[i];
			if (!StringUtils.isBlank(ligne)) {
				b.append(escapeChars(ligne));
				-- nbLignesNonVides;
				if (nbLignesNonVides > 0) {
					b.append(CR);
				}
			}
		}
		b.append(DOUBLE_QUOTE);
		return b.toString();
	}

	public static InputStream getInputStream(String csvContent) throws IOException {
		return new ByteArrayInputStream(csvContent.getBytes(CHARSET));
	}
}
