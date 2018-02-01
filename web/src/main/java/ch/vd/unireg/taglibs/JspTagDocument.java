package ch.vd.uniregctb.taglibs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import ch.vd.uniregctb.document.Document;

/**
 * Tag jsp permettant d'afficher un icon du document en fonction de son type et de le télécharger
 */
public class JspTagDocument extends BodyTagSupport {

	private static final long serialVersionUID = 1749276428095102594L;

	private Document doc;

	@Override
	public int doStartTag() throws JspException {
		try {
			HttpServletRequest request = (HttpServletRequest) this.pageContext.getRequest();
			JspWriter out = pageContext.getOut();
			out.print(buidHtlm(request, doc));
			return SKIP_BODY;
		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}

	public static String buidHtlm(HttpServletRequest request, Document doc) {

		if (doc == null) {
			return "";
		}

		final String downloadURL = request.getContextPath() + "/common/docs/download.do?id=" + doc.getId() + "&url_memorize=false";
		final String imageURL = request.getContextPath() + "/images/" + doc.getFileExtension() + "_icon.png";

		return "<a href=\"" + downloadURL + "\">" +
				"<img src=\"" + imageURL + "\" align=\"top\" id=\"IMG_DOC_" + doc.getId() + "\">" +
				"</a>";
	}

	public void setDoc(Document doc) {
		this.doc = doc;
	}
}
