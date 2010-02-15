package ch.vd.uniregctb.rapport;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;

import ch.vd.uniregctb.common.JobResults;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;

import ch.vd.uniregctb.common.ApplicationInfo;
import ch.vd.uniregctb.common.AuthenticationHelper;

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
		PdfTableSimple t = new PdfTableSimple(2);
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
	public void attacheFichier(PdfWriter writer, String filename, String description, String contenu, float x) throws DocumentException {

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
	 * Ajoute un paragraphe listant de manière détaillé un résultat de processing.
	 */
	public void addListeDetaillee(PdfWriter writer, int size, String titre, String listVide, String filename, String contenu)
			throws DocumentException {

		Paragraph entete = new Paragraph();
		entete.setSpacingBefore(10);
		entete.add(new Chunk(titre, entete1));
		add(entete);

		Paragraph details = new Paragraph();
		details.setIndentationLeft(50);
		details.setSpacingBefore(10);
		details.setSpacingAfter(10);
		details.setFont(normalFont);
		{
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
}
