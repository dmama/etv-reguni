package ch.vd.unireg.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

import ch.vd.unireg.utils.UniregModeHelper;

public class JspTagTestMode extends TagSupport {

	private static final long serialVersionUID = 7341126434765629518L;
	
	@Override
	public int doStartTag() throws JspTagException {
		if (UniregModeHelper.isTestMode()) {
			return Tag.EVAL_BODY_INCLUDE;
		}
		
		return Tag.SKIP_BODY;
	}
}