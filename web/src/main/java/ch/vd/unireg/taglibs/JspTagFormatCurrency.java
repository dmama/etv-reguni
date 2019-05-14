package ch.vd.unireg.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import ch.vd.unireg.common.UniregLocale;

public class JspTagFormatCurrency extends BodyTagSupport {

	private static final long serialVersionUID = -8926945899L;

	private static final NumberFormat CURRENCY_FORMAT;

	static {
		CURRENCY_FORMAT = NumberFormat.getInstance(new Locale("fr", "CH"));
		((DecimalFormat) CURRENCY_FORMAT).setDecimalFormatSymbols(UniregLocale.SYMBOLS);
	}

	private Object value;

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

	public void setValue(Object value) {
		this.value = value;
	}

	String buidHtlm() {
		return CURRENCY_FORMAT.format(value);
	}
}
