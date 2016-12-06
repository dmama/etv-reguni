package ch.vd.uniregctb.common;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.StatusManager;

/**
 * Classe utilitaire qui est utilisée pour générer des fichiers CSV
 */
public abstract class CsvHelper {

	public static final char COMMA = ';';
	public static final String CR = "\n";
	public static final char DOUBLE_QUOTE = '"';
	public static final String EMPTY = StringUtils.EMPTY;
	public static final String CHARSET = "ISO-8859-15";
	public static final String MIME_TYPE = MimeTypeHelper.MIME_CSV + ";charset=" + CHARSET;

	public interface LineFiller {
		LineFiller append(int i);
		LineFiller append(long l);
		LineFiller append(char c);
		LineFiller append(CharSequence s);
		LineFiller append(RegDate d);
		LineFiller append(Object o);
	}

	/**
	 * Interface implémentée par le code spécifique au remplissage d'un fichier CSV
	 * @param <T>
	 */
	public interface FileFiller<T> {
		/**
		 * Remplissage de la ligne d'entête (sans le CR final)
		 * @param b destination du remplissage
		 */
		void fillHeader(LineFiller b);

		/**
		 * Remplissage de chacune des lignes (sans le CR final)
		 * @param b destination du remplissage
		 * @param elt source de l'information à utiliser pour le remplissage
		 * @return <code>true</code> si quelque chose a été ajouté, <code>false</code> sinon (afin de savoir si on doit ajouter un CR)
		 */
		boolean fillLine(LineFiller b, T elt);
	}

	private static final class WriterLineFiller implements LineFiller {

		private final Writer w;

		public WriterLineFiller(Writer w) {
			this.w = w;
		}

		@Override
		public LineFiller append(int i) {
			appendCharSequence(Integer.toString(i));
			return this;
		}

		@Override
		public LineFiller append(long l) {
			appendCharSequence(Long.toString(l));
			return this;
		}

		@Override
		public LineFiller append(char c) {
			try {
				w.append(c);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
			return this;
		}

		@Override
		public LineFiller append(CharSequence s) {
			appendCharSequence(s);
			return this;
		}

		@Override
		public LineFiller append(RegDate d) {
			appendCharSequence(RegDateHelper.dateToDashString(d));
			return this;
		}

		@Override
		public LineFiller append(Object o) {
			appendCharSequence(String.format("%s", o));
			return this;
		}

		private void appendCharSequence(CharSequence s) {
			try {
				w.append(s);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Nullable("si la collection est vide")
	public static <T> TemporaryFile asCsvTemporaryFile(Collection<T> list, String fileName, @Nullable StatusManager status, FileFiller<? super T> filler) {
		TemporaryFile file = null;
		if (!list.isEmpty()) {
			try {
				file = new TemporaryFile("ur-csv-");
				try (final OutputStream out = file.openOutputStream();
				     final OutputStreamWriter writer = new OutputStreamWriter(out, CHARSET);
				     final BufferedWriter bufferedWriter = new BufferedWriter(writer, 1024 * 1024)) {

					final LineFiller lf = new WriterLineFiller(bufferedWriter);
					buildFileContent(list, fileName, status, filler, lf);
				}
			}
			catch (RuntimeException | Error e) {
				if (file != null) {
					file.close();
				}
				throw e;
			}
			catch (IOException e) {
				if (file != null) {
					file.close();
				}
				throw new RuntimeException(e);
			}
		}
		return file;
	}

	@Nullable("si l'itérateur est vide")
	public static <T> TemporaryFile asCsvTemporaryFile(@NotNull Iterator<T> iter, String fileName, @Nullable StatusManager status, FileFiller<? super T> filler) {
		TemporaryFile file = null;
		try {
			file = new TemporaryFile("ur-csv-");
			try (final OutputStream out = file.openOutputStream();
			     final OutputStreamWriter writer = new OutputStreamWriter(out, CHARSET);
			     final BufferedWriter bufferedWriter = new BufferedWriter(writer, 1024 * 1024)) {

				final LineFiller lf = new WriterLineFiller(bufferedWriter);
				final long lines = buildFileContent(iter, fileName, status, filler, lf);
				if (lines == 0) {
					return null;
				}
			}
		}
		catch (RuntimeException | Error e) {
			if (file != null) {
				file.close();
			}
			throw e;
		}
		catch (IOException e) {
			if (file != null) {
				file.close();
			}
			throw new RuntimeException(e);
		}
		return file;
	}

	private static <T> void buildFileContent(Collection<T> list, String fileName, StatusManager status, FileFiller<? super T> filler, LineFiller lf) {
		filler.fillHeader(lf);
		lf.append(CR);

		final String message = String.format("Génération du fichier %s", fileName);
		if (status != null) {
			status.setMessage(message, 0);
		}

		final GentilIterator<T> iter = new GentilIterator<>(list);
		while (iter.hasNext()) {
			final T info = iter.next();
			if (iter.isAtNewPercent() && status != null) {
				status.setMessage(message, iter.getPercent());
			}
			if (filler.fillLine(lf, info)) {
				lf.append(CR);
			}
		}
	}

	private static <T> long buildFileContent(Iterator<T> iter, String fileName, StatusManager status, FileFiller<? super T> filler, LineFiller lf) {
		filler.fillHeader(lf);
		lf.append(CR);

		final String message = String.format("Génération du fichier %s", fileName);
		if (status != null) {
			status.setMessage(message, 0);
		}

		long lines = 0;
		while (iter.hasNext()) {
			final T info = iter.next();
			if (filler.fillLine(lf, info)) {
				lf.append(CR);
			}
			++lines;
		}

		return lines;
	}

	/**
	 * Transforme la ligne spécifiée (qui peut contenir des retours de lignes embeddés) en une chaîne de caractère capable de tenir dans un champ d'un fichier CSV. Les retours de lignes sont préservés,
	 * mais les éventuels caractères interdits (" et ;) sont supprimés.
	 * @param lignes chaîne de caractères potentiellement sur plusieurs lignes
	 */
	public static String asCsvField(String lignes) {
		if (lignes == null) {
			return EMPTY;
		}
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
	 * Supression des caractères " et ;, et remplacement de tous les caractères "blancs" par un espace
	 * @param ligne
	 * @return
	 */
	public static String escapeChars(String ligne) {
		return StringUtils.isBlank(ligne) ? EMPTY : ligne.replaceAll("[\";]", EMPTY).replaceAll("[\\s\\u00a0]+", " ").trim();
	}

	/**
	 * Transforme les lignes spécifiées en une chaîne de caractère capable de tenir dans un champ d'un fichier CSV. Les retours de lignes sont préservés, mais les éventuels caractères interdits (" et ;)
	 * sont supprimés.
	 */
	public static String asCsvField(String[] lignes) {
		if (lignes != null && lignes.length > 0) {
			final StringBuilder b = new StringBuilder();
			final int length = lignes.length;

			// compte les lignes non-vides
			int nbLignesNonVides = 0;
			for (int i = 0 ; i < length ; ++ i) {
				if (!StringUtils.isBlank(lignes[i])) {
					++ nbLignesNonVides;
				}
			}

			// construit la chaîne de caractères
			if (nbLignesNonVides > 0) {
				b.append(DOUBLE_QUOTE);
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
			}
			return b.toString();
		}
		else {
			return EMPTY;
		}
	}
}
