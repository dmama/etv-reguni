package ch.vd.uniregctb.rapport;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.*;

import org.apache.commons.lang.StringUtils;
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
	private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("dd.MM.yyyy kk:mm:ss");
	public static final int AVG_LINE_LEN = 384; // longueur moyenne d'une ligne d'un fichier CVS (d'après relevé sur le batch d'ouverture des fors)
	public static final char COMMA = ';';
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
		t.getDefaultCell().setBorder(Cell.NO_BORDER);
		callback.fillTable(t);
		add(t);
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
		addPartieDeListeDetaillee(writer, size, titre, listVide, filename, contenu);
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
	private void addPartieDeListeDetaillee(PdfWriter writer, int size, String titre, String listVide, String filename, String contenu) throws DocumentException {
		final Paragraph details = new Paragraph();
		details.setIndentationLeft(50);
		details.setSpacingBefore(10);
		details.setSpacingAfter(10);
		details.setFont(normalFont);

		if (size == 0) {
			details.add(new Chunk(listVide));
		}
		else {
			details.add(new Chunk("(voir le fichier attaché " + filename + ")"));
		}

		add(details);

		if (size > 0) {
			Assert.notNull(contenu);
			attacheFichier(writer, filename, titre, contenu);
		}
	}

	protected void addListeDetailleeDecoupee(PdfWriter writer, int size, String titre, String listVide, String[] filenames, String[] contenus) throws DocumentException {
		Assert.isEqual(filenames.length, contenus.length);

		addEnteteListeDetaillee(titre);
		if (filenames.length > 0) {
			for (int i = 0 ; i < filenames.length ; ++ i) {
				addPartieDeListeDetaillee(writer, size, titre, listVide, filenames[i], contenus[i]);
			}
		}
		else {
			addPartieDeListeDetaillee(writer, size, titre, listVide, "empty-file", "");
		}
	}

	/**
	 * Construit le contenu du fichier détaillé des contribuables traités
	 */
	protected static String ctbIdsAsCsvFile(List<Long> ctbsTraites, String filename, StatusManager status) {
		String contenu = null;
		int size = ctbsTraites.size();
		if (size > 0) {
			StringBuilder b = new StringBuilder("Numéro de contribuable\n");

			final GentilIterator<Long> iter = new GentilIterator<Long>(ctbsTraites);
			while (iter.hasNext()) {
				if (iter.isAtNewPercent()) {
					status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
				}
				b.append(iter.next());
				if (!iter.isLast()) {
					b.append("\n");
				}
			}
			contenu = b.toString();
		}
		return contenu;
	}

	/**
	 * Traduit la liste d'infos en un fichier CSV
	 */
	protected static <T extends JobResults.Info> String asCsvFile(List<T> list, String filename, StatusManager status) {
		String contenu = null;
		int size = list.size();
		if (size > 0) {

			StringBuilder b = new StringBuilder(AVG_LINE_LEN * list.size());
			b.append("Numéro de l'office d'impôt").append(COMMA).append("Numéro de contribuable").append(COMMA + "Nom du contribuable")
					.append(COMMA).append("Raison").append(COMMA).append("Commentaire\n");

			final GentilIterator<T> iter = new GentilIterator<T>(list);
			while (iter.hasNext()) {
				if (iter.isAtNewPercent()) {
					status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
				}

				T info = iter.next();
				StringBuilder bb = new StringBuilder(AVG_LINE_LEN);
				bb.append(info.officeImpotID).append(COMMA);
				bb.append(info.noCtb).append(COMMA);
				bb.append(escapeChars(info.nomCtb)).append(COMMA);
				bb.append(info.getDescriptionRaison());
				if (info.details != null) {
					bb.append(COMMA).append(asCsvField(info.details));
				}
				if (!iter.isLast()) {
					bb.append("\n");
				}

				b.append(bb);
			}
			contenu = b.toString();
		}
		return contenu;
	}

	/**
	 * Transforme la ligne spécifiée (qui peut contenir des retours de lignes embeddés) en une chaîne de caractère capable de tenir dans un champ d'un fichier CSV. Les retours de lignes sont préservés,
	 * mais les éventuels caractères interdits (" et ;) sont supprimés.
	 */
	private static String asCsvField(String lignes) {
		return asCsvField(lignes.split("\n"));
	}

	/**
	 * Supression des caractères " et ;
	 * @param ligne
	 * @return
	 */
	protected static String escapeChars(String ligne) {
		return StringUtils.isBlank(ligne) ? EMPTY : ligne.trim().replaceAll("[\";]", EMPTY);
	}

	/**
	 * Transforme les lignes spécifiées en une chaîne de caractère capable de tenir dans un champ d'un fichier CSV. Les retours de lignes sont préservés, mais les éventuels caractères interdits (" et ;)
	 * sont supprimés.
	 */
	protected static String asCsvField(String[] lignes) {
		final StringBuilder b = new StringBuilder();
		b.append("\"");
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
					b.append("\n");
				}
			}
		}
		b.append("\"");
		return b.toString();
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
		final long start = results.startTime;
		final long end = results.endTime == 0 ? System.currentTimeMillis() : results.endTime;
		final long milliseconds = end - start;

		final int seconds = (int) ((milliseconds / 1000) % 60);
		final int minutes = (int) ((milliseconds / 1000) / 60) % 60;
		final int hours = (int) ((milliseconds / 1000) / 3600) % 24;
		final int days = (int) ((milliseconds / 1000) / (3600 * 24));

		return formatDureeExecution(days, hours, minutes, seconds);
	}

	/**
	 * Formatte une durée d'exécution d'un rapport sous la forme <i>1 jour, 0 heure, 23 minutes et 1 seconde</i>.
	 *
	 * @param days    le nombre de jours
	 * @param hours   le nombre d'heures (0-23)
	 * @param minutes le nombre de minutes (0-59)
	 * @param seconds le nombre de secondes (0-59)
	 * @return une string représentant la durée sous forme humaine.
	 */
	protected static String formatDureeExecution(int days, int hours, int minutes, int seconds) {

		StringBuilder s = new StringBuilder();

		if (days > 0) {
			s.append(days).append(' ').append(pluralize(days, "jour")).append(", ");
		}
		if (days > 0 || hours > 0) {
			s.append(hours).append(' ').append(pluralize(hours, "heure")).append(", ");
		}
		if (days > 0 || hours > 0 || minutes > 0) {
			s.append(minutes).append(' ').append(pluralize(minutes, "minute")).append(" et ");
		}
		s.append(seconds).append(' ').append(pluralize(seconds, "seconde"));

		return s.toString();
	}

	/**
	 * Implémentation très stupide de la méthode pluralize (inspirée de Ruby on Rails) qui ajoute un 's' à la fin du mot singulier lorsque count > 1.
	 *
	 * @param count    le nombre d'occurences
	 * @param singular la version au singulier du mot
	 * @return la version au singulier ou au pluriel du mot en fonction du nombre d'occurences.
	 */
	protected static String pluralize(int count, String singular) {
		if (count > 1) {
			return singular + 's';
		}
		else {
			return singular;
		}
	}

	protected static String idsCtbsAsCsvFile(List<Long> ctbsIds, String filename, StatusManager status) {
		String contenu = null;
		int size = ctbsIds.size();
		if (size > 0) {

			StringBuilder b = new StringBuilder(AVG_LINE_LEN * ctbsIds.size());
			b.append("Numéro de contribuable\n");

			final GentilIterator<Long> iter = new GentilIterator<Long>(ctbsIds);
			while (iter.hasNext()) {
				if (iter.isAtNewPercent()) {
					status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
				}

				Long id = iter.next();
				b.append(id).append('\n');
			}
			contenu = b.toString();
		}
		return contenu;
	}
}
