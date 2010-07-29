package ch.vd.uniregctb.taglibs;

import java.util.Date;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import ch.vd.registre.base.date.DateHelper;

public class JspTagFormatDateToString extends BodyTagSupport {

	private static final long serialVersionUID = -89L;

	private Date sdate;

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

	public void setSdate(Date sdate) {
		this.sdate = sdate;
	}

	private String buidHtlm() {

		return DateHelper.dateTimeToDisplayString(sdate);
	}

}
