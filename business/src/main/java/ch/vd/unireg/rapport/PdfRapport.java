package ch.vd.unireg.rapport;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfAnnotation;
import com.itextpdf.text.pdf.PdfFileSpecification;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.codec.PngImage;
import org.apache.commons.lang3.StringUtils;

import ch.vd.unireg.common.AbstractJobResults;
import ch.vd.unireg.common.ApplicationInfo;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.JobResults;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.common.TimeHelper;

/**
 * Classe de base des rapports au format PDF du projet Unireg.
 */
public abstract class PdfRapport extends Document {

	private static final Font WARNING_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
	private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	public static final char COMMA = CsvHelper.COMMA;
	public static final String EMPTY = StringUtils.EMPTY;

	protected final Font titreFont = new Font(Font.FontFamily.HELVETICA, 20);
	protected final Font entete1 = WARNING_FONT;
	protected final Font normalFont = new Font(Font.FontFamily.HELVETICA, 10);

	public PdfRapport() {
		super(PageSize.A4);
	}

	/**
	 * Défini le nom et la description du document qui seront visibles depuis le browser de fichiers.
	 */
	public void addMetaInfo(String nom, String description) {
		addTitle(nom);
		addAuthor(AuthenticationHelper.getCurrentPrincipal());
		addSubject(description);
		addCreator(ApplicationInfo.getName() + " / " + ApplicationInfo.getVersion());
	}

	/**
	 * Ajoute la bannière d'entête Unireg.
	 */
	public void addEnteteUnireg() throws DocumentException {
		add(getPngImage("rapport/entete_unireg.png"));
	}

	/**
	 * Ajoute un titre principal au document.
	 */
	public void addTitrePrincipal(String titre) throws DocumentException {
		Paragraph p = new Paragraph(titre, titreFont);
		p.setAlignment(Element.ALIGN_CENTER);
		p.setSpacingBefore(10);
		p.setSpacingAfter(20);
		add(p);
	}

	/**
	 * Ajoute une entête de niveau 1
	 */
	public void addEntete1(String entete) throws DocumentException {
		Paragraph params = new Paragraph();
		params.setFont(normalFont);
		params.setSpacingBefore(10);
		params.setSpacingAfter(10);
		params.add(new Chunk(entete, entete1));
		add(params);
	}

	/**
	 * Ajoute un paragraphe de warning: texte gras sur fond rouge.
	 */
	public void addWarning(String warning) throws DocumentException {
		Paragraph p = new Paragraph();
		Chunk c = new Chunk(warning, WARNING_FONT);
		c.setBackground(BaseColor.RED);
		p.setAlignment(Element.ALIGN_CENTER);
		p.setSpacingAfter(10);
		p.add(c);
		add(p);
	}

	public interface TableSimpleCallback {
		void fillTable(PdfTableSimple table) throws DocumentException;
	}

	/**
	 * Ajoute une table toute simple (sans bord ni attributs spéciaux).
	 */
	public void addTableSimple(int columns, TableSimpleCallback callback) throws DocumentException {
		final PdfTableSimple t = new PdfTableSimple(columns);
		fillTableSimple(t, callback);
	}

	public void addTableSimple(float[] widths, TableSimpleCallback callback) throws DocumentException {
		final PdfTableSimple t = new PdfTableSimple(widths);
		fillTableSimple(t, callback);
	}

	protected void fillTableSimple(PdfTableSimple table, TableSimpleCallback callback) throws DocumentException {
		table.getDefaultCell().setBorder(Rectangle.NO_BORDER);
		callback.fillTable(table);
		add(table);
	}

	/**
	 * Charge un image PNG iText depuis une ressource
	 */
	protected Image getPngImage(String path) throws DocumentException {
		try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
			return PngImage.getImage(is);
		}
		catch (IOException e) {
			throw new DocumentException(e);
		}
	}

	/**
	 * Attache un fichier au document PDF et ajoute une référence sur celui-ci à l'abscisse <i>x</i> de la ligne courante du document.
	 * (le contenu ajouté est présent dans le fichier sur disque indiqué)
	 */
	protected static void attacheFichier(PdfWriter writer, File fichier, String filename, String description, String mimeType, float x) throws IOException {
		final PdfFileSpecification file = PdfFileSpecification.fileEmbedded(writer, fichier.getPath(), filename, null, mimeType, null, 9);
		attacheFichier(writer, file, description, x);
	}

	/**
	 * Attache un fichier au document PDF et ajoute une référence sur celui-ci à l'abscisse <i>x</i> de la ligne courante du document.
	 */
	private static void attacheFichier(PdfWriter writer, PdfFileSpecification file, String description, float x) throws IOException {
		final float y = writer.getVerticalPosition(false);
		final Rectangle position = new Rectangle(x, y, x + 20f, y + 20f);
		writer.addAnnotation(PdfAnnotation.createFileAttachment(writer, position, description, file));
	}

	/**
	 * Ajoute un paragraphe listant de manière détaillée un résultat de processing.
	 */
	protected void addListeDetaillee(PdfWriter writer, String titre, String listVide, String filename, TemporaryFile contenu) throws DocumentException {
		addEnteteListeDetaillee(titre);
		addPartieDeListeDetaillee(writer, titre, listVide, filename, contenu);
	}

	private void addEnteteListeDetaillee(String titre) throws DocumentException {
		final Paragraph entete = new Paragraph();
		entete.setSpacingBefore(10);
		entete.add(new Chunk(titre, entete1));
		add(entete);
	}

	/**
	 * Ajoute un lien vers un fichier de détails
	 */
	private void addPartieDeListeDetaillee(PdfWriter writer, String titre, String descriptionListeVide, String filename, TemporaryFile contenu) throws DocumentException {
		final Paragraph details = new Paragraph();
		details.setIndentationLeft(50);
		details.setSpacingBefore(10);
		details.setSpacingAfter(10);
		details.setFont(normalFont);

		final boolean vide = contenu == null;
		if (vide) {
			details.add(new Chunk(descriptionListeVide));
		}
		else {
			details.add(new Chunk("(voir le fichier attaché " + filename + ')'));
		}

		add(details);

		if (!vide) {
			try {
				attacheFichier(writer, contenu.getFullPath(), filename, titre, CsvHelper.MIME_TYPE, 50);
			}
			catch (IOException e) {
				throw new DocumentException(e);
			}
		}
	}

	protected void addListeDetailleeDecoupee(PdfWriter writer, String titre, String listVide, String[] filenames, TemporaryFile[] contenus) throws DocumentException {
		if (filenames.length != contenus.length) {
			throw new IllegalArgumentException();
		}

		addEnteteListeDetaillee(titre);
		if (filenames.length > 0) {
			for (int i = 0; i < filenames.length; ++i) {
				addPartieDeListeDetaillee(writer, titre, listVide, filenames[i], contenus[i]);
			}
		}
		else {
			addPartieDeListeDetaillee(writer, titre, listVide, "empty-file", null);
		}
	}

	/**
	 * Construit le contenu du fichier détaillé des contribuables traités
	 */
	protected static TemporaryFile ctbIdsAsCsvFile(List<Long> ctbsTraites, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(ctbsTraites, filename, status, new CsvHelper.FileFiller<Long>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA).append("PERIODE_FISCALE").append(COMMA).append("STATUT").append(COMMA).append("PERIODE_FISCALE").append(COMMA).append("PERIODE_FISCALE");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, Long elt) {
				b.append(elt);
				return true;
			}
		});
	}

	/**
	 * Traduit la liste d'infos en un fichier CSV
	 */
	protected static <T extends JobResults.Info> TemporaryFile asCsvFile(List<T> list, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(list, filename, status, new CsvHelper.FileFiller<T>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("OID").append(COMMA).append("NO_CTB").append(COMMA).append("NOM")
						.append(COMMA).append("RAISON").append(COMMA).append("COMMENTAIRE");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, T elt) {
				b.append(elt.officeImpotID != null ? elt.officeImpotID.toString() : EMPTY).append(COMMA);
				b.append(elt.noCtb).append(COMMA);
				b.append(escapeChars(elt.nomCtb)).append(COMMA);
				b.append(escapeChars(elt.getDescriptionRaison()));
				if (elt.details != null) {
					b.append(COMMA).append(asCsvField(elt.details));
				}
				return true;
			}
		});
	}

	/**
	 * Transforme la ligne spécifiée (qui peut contenir des retours de lignes embeddés) en une chaîne de caractère capable de tenir dans un champ d'un fichier CSV. Les retours de lignes sont préservés,
	 * mais les éventuels caractères interdits (" et ;) sont supprimés.
	 */
	protected static String asCsvField(String lignes) {
		return CsvHelper.asCsvField(lignes);
	}

	/**
	 * Supression des caractères " et ;
	 * @param ligne
	 * @return
	 */
	protected static String escapeChars(String ligne) {
		return CsvHelper.escapeChars(ligne);
	}

	/**
	 * Transforme les lignes spécifiées en une chaîne de caractère capable de tenir dans un champ d'un fichier CSV. Les retours de lignes sont préservés, mais les éventuels caractères interdits (" et ;)
	 * sont supprimés.
	 */
	protected static String asCsvField(String[] lignes) {
		return CsvHelper.asCsvField(lignes);
	}

	/**
	 * Transforme les lignes spécifiées en une chaîne de caractère capable de tenir dans un champ d'un fichier CSV. Les retours de lignes sont préservés, mais les éventuels caractères interdits (" et ;)
	 * sont supprimés.
	 */
	protected static String asCsvField(List<String> lignes) {
		return CsvHelper.asCsvField(lignes);
	}

	protected static String formatTimestamp(final Date dateGeneration) {
		return TIMESTAMP_FORMAT.format(dateGeneration);
	}

	/**
	 * Formatte une durée d'exécution d'un rapport sous la forme <i>1 jour, 0 heure, 23 minutes et 1 seconde</i>.
	 *
	 * @param results le résultat d'exécution du rapport.
	 * @return une string représentant la durée sous forme humaine.
	 */
	protected static String formatDureeExecution(AbstractJobResults results) {
		final long milliseconds = getDureeExecution(results);
		return formatDureeExecution(milliseconds);
	}

	/**
	 * Calcule la durée d'exécution d'un job en millisecondes
	 * @param results le résultat d'exécution du job
	 * @return la durée d'exécution, en millisecondes
	 */
	protected static long getDureeExecution(AbstractJobResults results) {
		final long start = results.startTime;
		final long end = results.endTime == 0 ? System.currentTimeMillis() : results.endTime;
		return end - start;
	}

	/**
	 * Formatte une durée d'exécution d'un rapport sous la forme <i>1 jour, 0 heure, 23 minutes et 1 seconde</i>.
	 *
	 * @param milliseconds le nombre de milliseconds que le job à duré
	 * @return une string représentant la durée sous forme humaine.
	 */
	protected static String formatDureeExecution(long milliseconds) {
		return TimeHelper.formatDuree(milliseconds);
	}
}
