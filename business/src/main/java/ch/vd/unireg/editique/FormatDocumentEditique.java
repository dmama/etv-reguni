package ch.vd.uniregctb.editique;

/**
 * Format du retour éditique attendu
 */
public enum FormatDocumentEditique {
	PDF("pdf"),
	PCL("pcl"),
	AFP("apf"),
	TIF("tif");

	private final String code;

	FormatDocumentEditique(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
