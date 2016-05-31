package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

import ch.vd.uniregctb.utils.UniregModeHelper;

/**
 * Ce tag permet d'inclure conditionnellement un fragment de code JSP en fonction de si le mode reqdes est activé ou pas
 */
public class JspTagIfReqDes extends TagSupport {

	@Override
	public int doStartTag() throws JspTagException {
		if (UniregModeHelper.isReqdesEnabled()) {
			return Tag.EVAL_BODY_INCLUDE;
		}

		return Tag.SKIP_BODY;
	}

}
