package ch.vd.unireg.rapport;

import com.itextpdf.text.pdf.PdfPTable;

public class PdfTableSimple extends PdfPTable {

	private final int numColumns;

	public PdfTableSimple(int numColumns) {
		super(numColumns);
		this.numColumns = numColumns;
	}

	public PdfTableSimple(float[] widths) {
		super(widths);
		this.numColumns = widths.length;
	}

	public void addLigne(String... columns) {
		if (numColumns != columns.length) {
			throw new IllegalArgumentException();
		}
		for (String cell : columns) {
			addCell(cell);
		}
	}
}
