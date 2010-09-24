package ch.vd.uniregctb.export;

import org.displaytag.export.BaseExportView;

/**
 * Filter d'export DisplayTag. Exporte les donn�es vers le fomrat d'objets JSON.
 * Utilis� pour la construction des menus en ajax entre autres.
 *
 * @author <a href="mailto:abenaissi@cross-systems.ch">Akram BEN AISSI</a>
 */
public class JsonView extends BaseExportView {

	/**
	 * @see org.displaytag.export.BaseExportView#getDocumentStart()
	 */
	@Override
	protected String getDocumentStart() {
		return "{\"result\":[";
	}

	/**
	 * @see org.displaytag.export.BaseExportView#getRowStart()
	 */
	@Override
	protected String getRowStart() {
		return "[";
	}

	/**
	 * @see org.displaytag.export.BaseExportView#getCellStart()
	 */
	@Override
	protected String getCellStart() {
		return "\"";
	}

	/**
	 * @see org.displaytag.export.BaseExportView#getAlwaysAppendCellEnd()
	 */
	@Override
	protected boolean getAlwaysAppendCellEnd() {
		return false;
	}

	/**
	 * @see org.displaytag.export.BaseExportView#getCellEnd()
	 */
	@Override
	protected String getCellEnd() {
		return "\",";
	}

	/**
	 * @see org.displaytag.export.BaseExportView#getAlwaysAppendRowEnd()
	 */
	@Override
	protected boolean getAlwaysAppendRowEnd() {
		return true;
	}

	/**
	 * @see org.displaytag.export.BaseExportView#getRowEnd()
	 */
	@Override
	protected String getRowEnd() {
		return "\"],\n";
	}

	/**
	 * @see org.displaytag.export.BaseExportView#getDocumentEnd()
	 */
	@Override
	protected String getDocumentEnd() {
		return "[]]}";
	}

	/**
	 * @see org.displaytag.export.BaseExportView#getMimeType()
	 */
	public String getMimeType() {
		return "text/plain";
	}

	/**
	 * @see org.displaytag.export.BaseExportView#escapeColumnValue(java.lang.Object)
	 */
	@Override
	protected String escapeColumnValue(Object object) {
		return object.toString();
	}
}