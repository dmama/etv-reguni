package ch.vd.uniregctb.taglibs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.util.HtmlUtils;

import ch.vd.uniregctb.common.MimeTypeHelper;

public class JspTagDocumentIcon extends BodyTagSupport {

	/**
	 * Mapping entre un mimeType (clé) et une URI d'image (valeur)
	 */
	private static final Map<String, String> typeMapping;

	static {
		typeMapping = new HashMap<String, String>();
		typeMapping.put(MimeTypeHelper.MIME_CSV, "/images/csv_icon.png");
		typeMapping.put(MimeTypeHelper.MIME_PLAINTEXT, "/images/txt_icon.png");
		typeMapping.put(MimeTypeHelper.MIME_PDF, "/images/pdf_icon.png");
		typeMapping.put(MimeTypeHelper.MIME_XML, "/images/xml_icon.png");
		typeMapping.put(MimeTypeHelper.MIME_APPXML, "/images/xml_icon.png");
		typeMapping.put(MimeTypeHelper.MIME_ZIP, "/images/zip_icon.png");
		typeMapping.put(MimeTypeHelper.MIME_MSWORD, "/images/doc_icon.png");
		typeMapping.put(MimeTypeHelper.MIME_HPPCL, "/images/printer_icon.png");
		typeMapping.put(MimeTypeHelper.MIME_XPCL, "/images/printer_icon.png");
		typeMapping.put(MimeTypeHelper.MIME_PCL, "/images/printer_icon.png");
	}

	private String mimeType;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	@Override
	public int doStartTag() throws JspException {
		try {
			final HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
			final JspWriter out = pageContext.getOut();
			out.print(buildHtml(request));
			return SKIP_BODY;
		}
		catch (IOException e) {
			throw new JspTagException(e);
		}
	}

	private String buildHtml(HttpServletRequest request) {
		return buildHtml(request.getContextPath(), mimeType, HtmlUtils.htmlEscape("?"));
	}

	/**
	 * Méthode publique de construction d'une chaîne HTML qui pointe vers l'icône correspondant au type MIME donné
	 * @param contextPath context path issu de la requête HTTP
	 * @param mimeType type MIME pour lequel on veut afficher une icône
	 * @param htmlForUnknownType code HTML à afficher si le type MIME est inconnu chez nous
	 * @return une chaîne HTML qui va bien
	 */
	public static String buildHtml(String contextPath, String mimeType, String htmlForUnknownType) {
		final String image = getImageUri(contextPath, mimeType);
		final String html;
		if (image != null) {
			html = String.format("<img src='%s'>&nbsp;</img>", image);
		}
		else {
			html = htmlForUnknownType;
		}
		return html;
	}

	/**
	 * Méthode publique de récupération de l'URI de l'icône correspondant au type MIME donné
	 * @param contextPath context path issu de la requête HTTP
	 * @param mimeType type MIME pour lequel on veut afficher une icône
	 * @return URI de l'icône, ou <code>null</code> si le type MIME n'a pas d'icône associée connue
	 */
	public static String getImageUri(String contextPath, String mimeType) {
		final String type = StringUtils.trimToEmpty(mimeType);
		final String image = typeMapping.get(type);
		if (image != null) {
			return String.format("%s%s", contextPath, image);
		}
		return null;
	}
}
