package ch.vd.unireg.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import ch.vd.unireg.common.FormatNumeroHelper;

/**
 * Tag JSP qui permet de formatter un numéro cantonal.
 */
public class JspTagCantonalId extends BodyTagSupport {

	private static final long serialVersionUID = 3992933644995036027L;

	private String cantonalId;

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

	public void setCantonalId(String cantonalId) {
		this.cantonalId = cantonalId;
	}

	private String buildHtml() {
		return FormatNumeroHelper.formatCantonalId(cantonalId);
	}
}
