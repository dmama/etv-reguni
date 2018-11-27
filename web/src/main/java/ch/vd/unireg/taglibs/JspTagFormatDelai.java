package ch.vd.unireg.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import ch.vd.unireg.type.delai.Delai;
import ch.vd.unireg.type.delai.DelaiHelper;

/**
 * Tag pour formatter les valeurs {@link ch.vd.unireg.type.delai.Delai}.
 */
public class JspTagFormatDelai extends BodyTagSupport {

	private static final long serialVersionUID = -1674808565643612957L;

	private Delai value;

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

	public void setValue(Delai value) {
		this.value = value;
	}

	private String buidHtlm() {
		return DelaiHelper.toDisplayString(value);
	}
}
