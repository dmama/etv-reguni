package ch.vd.uniregctb.common;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * Méthodes utilitaires autour des types MIME
 */
public abstract class MimeTypeHelper {

	public static final String MIME_PCL = "application/pcl";
	public static final String MIME_XPCL = "application/x-pcl";
	public static final String MIME_HPPCL = "application/vnd.hp-pcl";
	public static final String MIME_CHVD = "application/x-chvd";
	public static final String MIME_CSV = "text/csv";
	public static final String MIME_PLAINTEXT = "text/plain";
	public static final String MIME_HTML = "text/html";
	public static final String MIME_PDF = "application/pdf";
	public static final String MIME_APPXML = "application/xml";
	public static final String MIME_XML = "text/xml";
	public static final String MIME_ZIP = "application/zip";
	public static final String MIME_MSWORD = "application/msword";
	public static final String MIME_TIFF = "image/tiff";
	public static final String MIME_AFP = "application/afp";

	private static final Map<String, String> fileSuffixes;

	static {
		fileSuffixes = new HashMap<>();
		fileSuffixes.put(MIME_PCL, ".pcl");
		fileSuffixes.put(MIME_XPCL, ".pcl");
		fileSuffixes.put(MIME_HPPCL, ".pcl");
		fileSuffixes.put(MIME_CHVD, ".chvd");
		fileSuffixes.put(MIME_CSV, ".csv");
		fileSuffixes.put(MIME_PLAINTEXT, ".txt");
		fileSuffixes.put(MIME_HTML, ".html");
		fileSuffixes.put(MIME_PDF, ".pdf");
		fileSuffixes.put(MIME_XML, ".xml");
		fileSuffixes.put(MIME_APPXML, ".xml");
		fileSuffixes.put(MIME_ZIP, ".zip");
		fileSuffixes.put(MIME_MSWORD, ".doc");
		fileSuffixes.put(MIME_TIFF, ".tiff");
		fileSuffixes.put(MIME_AFP, ".afp");
	}

	/**
	 * @param mimeType type MIME considéré
	 * @return l'extension (avec le point séparateur inclu) à utiliser pour un fichier dont le contenu est du type donné, une chaîne vide si le type est inconnu
	 */
	public static String getFileExtensionForType(String mimeType) {
		final String ext = fileSuffixes.get(mimeType);
		if (ext != null || mimeType == null || !mimeType.contains(";")) {
			return StringUtils.trimToEmpty(ext);
		}

		// si on est ici, c'est qu'on n'a pas trouvé d'extension pour le mimeType donné et que
		// le nom du mimetype contient un ";" (exemple : "text/plain; charset=UTF-8"
		// -> dans ce cas, on refait la recherche de l'extension sur la première partie du mimetype seulement
		return getFileExtensionForType(mimeType.substring(0, mimeType.indexOf(';')));
	}
}
