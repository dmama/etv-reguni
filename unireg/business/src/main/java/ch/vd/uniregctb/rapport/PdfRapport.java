package ch.vd.uniregctb.rapport;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.*;

import org.apache.log4j.Logger;

import com.lowagie.text.Cell;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfAnnotation;
import com.lowagie.text.pdf.PdfFileSpecification;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.codec.PngImage;

/**
 * Classe de base des rapports au format PDF du projet Unireg.
 */
public abstract class PdfRapport extends Document {

	private static final Font WARNING_FONT = new Font(Font.HELVETICA, 12, Font.BOLD);
	private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	public static final int AVG_LINE_LEN = 384; // longueur moyenne d'une ligne d'un fichier CVS (d'après relevé sur le batch d'ouverture des fors)
	public static final char COMMA = CsvHelper.COMMA;
	public static final String EMPTY = "";

	private final Logger LOGGER = Logger.getLogger(PdfRapport.class);

	protected final Font titreFont = new Font(Font.HELVETICA, 20);
	protected final Font entete1 = WARNING_FONT;
	protected final Font normalFont = new Font(Font.HELVETICA, 10);

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
		c.setBackground(Color.RED);
		p.setAlignment(Element.ALIGN_CENTER);
		p.setSpacingAfter(10);
		p.add(c);
		add(p);
	}

	public abstract interface TableSimpleCallback {
		abstract void fillTable(PdfTableSimple table) throws DocumentException;
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
		table.getDefaultCell().setBorder(Cell.NO_BORDER);
		callback.fillTable(table);
		add(table);
	}

	/**
	 * Charge un image PNG iText depuis une ressource
	 */
	protected Image getPngImage(String path) throws DocumentException {
		InputStream is = null;
		try {
			is = getClass().getClassLoader().getResourceAsStream(path);
			return PngImage.getImage(is);
		}
		catch (IOException e) {
			throw new DocumentException(e);
		}
		finally {
			if (is != null) {
				try {
					is.close();
				}
				catch (IOException e) {
					// que faire de plus ?
					LOGGER.error(e);
				}
			}
		}
	}

	/**
	 * Attache un fichier au document PDF et ajoute une référence sur celui-ci au <b>début</b> de la ligne courante du document.
	 */
	protected void attacheFichier(PdfWriter writer, String filename, String description, String contenu) throws DocumentException {

		float x = 50; // il n'y a pas moyen (= pas trouvé) de connaître la position X courante
		attacheFichier(writer, filename, description, contenu, x);
	}

	/**
	 * Attache un fichier au document PDF et ajoute une référence sur celui-ci au <b>début</b> de la ligne courante du document.
	 */
	protected void attacheFichier(PdfWriter writer, String filename, String description, String contenu, float x) throws DocumentException {

		float y = writer.getVerticalPosition(false);
		Rectangle position = new Rectangle(x, y, x + 20f, y + 20f);

		// Encoding ISO-8859-1 : parce qu'Excel ne reconnaît pas tout seul l'encoding des fichiers CSV...
		PdfFileSpecification file;
		try {
			file = PdfFileSpecification.fileEmbedded(writer, null, filename, contenu.getBytes("ISO-8859-1"), 9);
			writer.addAnnotation(PdfAnnotation.createFileAttachment(writer, position, description, file));
		}
		catch (Exception e) {
			throw new DocumentException(e);
		}
	}

	/**
	 * Ajoute un paragraphe listant de manière détaillée un résultat de processing.
	 */
	protected void addListeDetaillee(PdfWriter writer, int size, String titre, String listVide, String filename, String contenu) throws DocumentException {
		addEnteteListeDetaillee(titre);
		addPartieDeListeDetaillee(writer, size == 0, titre, listVide, filename, contenu);
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
	private void addPartieDeListeDetaillee(PdfWriter writer, boolean vide, String titre, String listVide, String filename, String contenu) throws DocumentException {
		final Paragraph details = new Paragraph();
		details.setIndentationLeft(50);
		details.setSpacingBefore(10);
		details.setSpacingAfter(10);
		details.setFont(normalFont);

		if (vide) {
			details.add(new Chunk(listVide));
		}
		else {
			details.add(new Chunk("(voir le fichier attaché " + filename + ")"));
		}

		add(details);

		if (!vide) {
			Assert.notNull(contenu);
			attacheFichier(writer, filename, titre, contenu);
		}
	}

	protected void addListeDetailleeDecoupee(PdfWriter writer, boolean vide, String titre, String listVide, String[] filenames, String[] contenus) throws DocumentException {
		Assert.isEqual(filenames.length, contenus.length);

		addEnteteListeDetaillee(titre);
		if (filenames.length > 0) {
			for (int i = 0 ; i < filenames.length ; ++ i) {
				addPartieDeListeDetaillee(writer, vide, titre, listVide, filenames[i], contenus[i]);
			}
		}
		else {
			addPartieDeListeDetaillee(writer, vide, titre, listVide, "empty-file", "");
		}
	}

	/**
	 * Construit le contenu du fichier détaillé des contribuables traités
	 */
	protected static String ctbIdsAsCsvFile(List<Long> ctbsTraites, String filename, StatusManager status) {
		return CsvHelper.asCsvFile(ctbsTraites, filename, status, 10, new CsvHelper.Filler<Long>() {
			public void fillHeader(StringBuilder b) {
				b.append("NO_CTB");
			}

			public void fillLine(StringBuilder b, Long elt) {
				b.append(elt);
			}
		});
	}

	/**
	 * Traduit la liste d'infos en un fichier CSV
	 */
	protected static <T extends JobResults.Info> String asCsvFile(List<T> list, String filename, StatusManager status) {
		return CsvHelper.asCsvFile(list, filename, status, AVG_LINE_LEN, new CsvHelper.Filler<T>() {
			public void fillHeader(StringBuilder b) {
				b.append("OID").append(COMMA).append("NO_CTB").append(COMMA).append("NOM")
						.append(COMMA).append("RAISON").append(COMMA).append("COMMENTAIRE");
			}

			public void fillLine(StringBuilder b, T elt) {
				b.append(elt.officeImpotID).append(COMMA);
				b.append(elt.noCtb).append(COMMA);
				b.append(escapeChars(elt.nomCtb)).append(COMMA);
				b.append(elt.getDescriptionRaison());
				if (elt.details != null) {
					b.append(COMMA).append(asCsvField(elt.details));
				}
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


	protected static String formatTimestamp(final Date dateGeneration) {
		return TIMESTAMP_FORMAT.format(dateGeneration);
	}

	/**
	 * Formatte une durée d'exécution d'un rapport sous la forme <i>1 jour, 0 heure, 23 minutes et 1 seconde</i>.
	 *
	 * @param results le résultat d'exécution du rapport.
	 * @return une string représentant la durée sous forme humaine.
	 */
	protected static String formatDureeExecution(JobResults results) {
		final long milliseconds = getDureeExecution(results);
		return formatDureeExecution(milliseconds);
	}

	/**
	 * Calcule la durée d'exécution d'un job en millisecondes
	 * @param results le résultat d'exécution du job
	 * @return la durée d'exécution, en millisecondes
	 */
	protected static long getDureeExecution(JobResults results) {
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
