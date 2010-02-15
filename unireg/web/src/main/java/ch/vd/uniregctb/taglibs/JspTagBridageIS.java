package ch.vd.uniregctb.taglibs;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

import ch.vd.uniregctb.utils.BridageISHelper;



public class JspTagBridageIS extends TagSupport{

	private static final long serialVersionUID = -5939374942030708248L;
	@Override
	public int doStartTag() throws JspTagException {
		if (BridageISHelper.isBridageIS()) {
			return Tag.SKIP_BODY;
		}

		return Tag.EVAL_BODY_INCLUDE;
	}

}
