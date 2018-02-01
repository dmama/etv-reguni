package ch.vd.unireg.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import ch.vd.unireg.common.FormatNumeroHelper;

public class JspTagNoIDE extends BodyTagSupport {

	private static final long serialVersionUID = 6586945068292373172L;

	private String numeroIDE;

	@Override
	public int doStartTag() throws JspTagException {
		try {
			final JspWriter out = pageContext.getOut();
			out.print(buildHtml());
			return SKIP_BODY;
		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}

	public void setNumeroIDE(String numeroIDE) {
		this.numeroIDE = numeroIDE;
	}

	private String buildHtml() {
		return FormatNumeroHelper.formatNumIDE(numeroIDE);
	}
}
