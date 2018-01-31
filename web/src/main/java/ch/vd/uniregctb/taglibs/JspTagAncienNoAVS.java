package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import ch.vd.uniregctb.common.FormatNumeroHelper;

public class JspTagAncienNoAVS extends BodyTagSupport {

	private static final long serialVersionUID = -8926945899L;

	private String ancienNumeroAVS;

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

	public void setAncienNumeroAVS(String ancienNumAVS) {
		this.ancienNumeroAVS = ancienNumAVS;
	}

	private String buidHtlm() {
		return FormatNumeroHelper.formatAncienNumAVS(ancienNumeroAVS);
	}

}
