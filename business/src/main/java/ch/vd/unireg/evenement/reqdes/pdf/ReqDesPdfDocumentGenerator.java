package ch.vd.uniregctb.evenement.reqdes.pdf;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;

import ch.vd.uniregctb.common.ApplicationInfo;

public abstract class ReqDesPdfDocumentGenerator {

	protected static final Font MAIN_TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 20);
	protected static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 14, Font.UNDERLINE);
	protected static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 10);
	protected static final Font TABLE_HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);

	protected static Document newDocument() {
		return new Document(PageSize.A4);
	}

	protected static void addMetaInformation(Document doc, String nomDocument, String description, String auteur) {
		doc.addTitle(nomDocument);
		doc.addAuthor(auteur);
		doc.addSubject(description);
		doc.addCreator(ApplicationInfo.getName() + " / " + ApplicationInfo.getVersion());
	}

	protected static void addTitrePrincipal(Document doc, String title) throws DocumentException {
		final Paragraph p = new Paragraph(title, MAIN_TITLE_FONT);
		p.setAlignment(Element.ALIGN_CENTER);
		p.setSpacingBefore(10);
		p.setSpacingBefore(20);
		doc.add(p);
	}

	protected void addEntete(Document doc, String entete) throws DocumentException {
		final Paragraph p = new Paragraph(entete, TITLE_FONT);
		p.setSpacingBefore(10);
		p.setSpacingAfter(10);
		doc.add(p);
	}
}
