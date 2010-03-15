package ch.vd.uniregctb.rapport;

import com.lowagie.text.pdf.PdfPTable;

public class PdfTableSimple extends PdfPTable {

	public PdfTableSimple(int numColumns) {
		super(numColumns);
	}

	public void addLigne(String left, String right) {
		addCell(left);
		addCell(right);
	}
}
