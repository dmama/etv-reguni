package ch.vd.unireg.common;

public class CsvView extends org.displaytag.export.CsvView {
	
	@Override
	protected String getCellEnd() {
		return ";";
	}

}
