package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

import ch.vd.uniregctb.utils.TestModeHelper;

public class JspTagTestMode extends TagSupport {

	private static final long serialVersionUID = 7341126434765629518L;
	
	@Override
	public int doStartTag() throws JspTagException {
		if (TestModeHelper.isTestMode()) {
			return Tag.EVAL_BODY_INCLUDE;
		}
		
		return Tag.SKIP_BODY;
	}
}
