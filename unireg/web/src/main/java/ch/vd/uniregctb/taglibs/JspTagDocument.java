package ch.vd.uniregctb.taglibs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import ch.vd.uniregctb.document.Document;
import ch.vd.uniregctb.web.HtmlTextWriter;
import ch.vd.uniregctb.web.HtmlTextWriterAttribute;
import ch.vd.uniregctb.web.HtmlTextWriterTag;
import ch.vd.uniregctb.web.io.StringWriter;

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

		StringWriter w = new StringWriter();
		HtmlTextWriter writer = new HtmlTextWriter(w);

		// Un lien permettant de télécharger le document
		String href = request.getContextPath() + "/common/docs.do?action=download&id=" + doc.getId();
		writer.addAttribute(HtmlTextWriterAttribute.Href, href);
		writer.renderBeginTag(HtmlTextWriterTag.A);
		{
			// Un image représentant le type de document
			String src = request.getContextPath() + "/images/" + doc.getFileExtension() + "_icon.png";
			writer.addAttribute(HtmlTextWriterAttribute.Src, src);
			writer.addAttribute(HtmlTextWriterAttribute.Align, "top");
			writer.addAttribute(HtmlTextWriterAttribute.Id, "IMG_DOC_" + doc.getId());
			writer.renderBeginTag(HtmlTextWriterTag.Img);
			writer.renderEndTag();
		}
		writer.renderEndTag();

		return w.toString();
	}

	public void setDoc(Document doc) {
		this.doc = doc;
	}
}
