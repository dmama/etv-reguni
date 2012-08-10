package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.RegDate;

@SuppressWarnings("UnusedDeclaration")
public class JspTagFormatRegDate extends BodyTagSupport {

	private static final long serialVersionUID = 9111856838695121236L;

	private RegDate regdate;
	private String format = "dd.MM.yyyy";
	private String defaultValue = StringUtils.EMPTY;

	@Override
	public int doStartTag() throws JspTagException {
		try {
			JspWriter out = pageContext.getOut();
			out.print(buidHtlm());
			return SKIP_BODY;
		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}

	public void setRegdate(RegDate regdate) {
		this.regdate = regdate;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	private String buidHtlm() {
		if (regdate == null) {
			return defaultValue;
		}
		String date = format;
		date = date.replaceAll("dd", String.format("%02d",regdate.day()));
		date = date.replaceAll("MM", String.format("%02d",regdate.month()));
		date = date.replaceAll("yyyy", String.format("%04d",regdate.year()));
		return date;
	}

}
