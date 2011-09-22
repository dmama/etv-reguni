package ch.vd.uniregctb.rapport;

import com.lowagie.text.pdf.PdfPTable;

import ch.vd.registre.base.utils.Assert;

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
		Assert.isEqual(numColumns, columns.length);
		for (String cell : columns) {
			addCell(cell);
		}
	}
}
