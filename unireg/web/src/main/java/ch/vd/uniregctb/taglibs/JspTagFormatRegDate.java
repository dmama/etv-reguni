package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

public class JspTagFormatRegDate extends BodyTagSupport {

	private static final long serialVersionUID = -1408963527869649422L;

	private RegDate regdate;
	private RegDateHelper.StringFormat format = RegDateHelper.StringFormat.DISPLAY;

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

	public void setRegdate(RegDate regdate) {
		this.regdate = regdate;
	}

	public void setFormat(String formatName) {
		format = RegDateHelper.StringFormat.valueOf(formatName);
	}

	private String buidHtlm() {
		return format.toString(regdate);
	}

}
