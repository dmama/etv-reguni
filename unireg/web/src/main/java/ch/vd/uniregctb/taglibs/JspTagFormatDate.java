package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import ch.vd.uniregctb.common.FormatNumeroHelper;

public class JspTagFormatDate extends BodyTagSupport {

	private static final long serialVersionUID = -8926945899L;

	private String date;

	@Override
	public int doStartTag() throws JspTagException {
		try {
			JspWriter out = pageContext.getOut();
			out.print(buidHtlm());
			// Skips the body.
			return SKIP_BODY;
		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}

	public void setDate(String date) {
		this.date = date;
	}

	private String buidHtlm() {

		return FormatNumeroHelper.formatDate(date);
	}

}
