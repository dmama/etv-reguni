package ch.vd.unireg.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.DayMonthHelper;

/**
 * Tag pour formatter les valeurs {@link DayMonth}.
 */
public class JspTagFormatDayMonth extends BodyTagSupport {

	private static final long serialVersionUID = -3857843560971315320L;

	private DayMonth value;

	@Override
	public int doStartTag() throws JspTagException {
		try {
			JspWriter out = pageContext.getOut();
			if (value != null) {
				out.print(buidHtlm());
			}
			return SKIP_BODY;
		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}

	public void setValue(DayMonth value) {
		this.value = value;
	}

	private String buidHtlm() {
		return DayMonthHelper.toDisplayString(value);
	}
}
