package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

import ch.vd.uniregctb.utils.UniregModeHelper;

/**
 * Ce tag permet d'inclure conditionnellement un fragment de code JSP en fonction de si le mode efacture est activé ou pas
 */
public class JspTagIfEfacture extends TagSupport {

	@Override
	public int doStartTag() throws JspTagException {
		if (UniregModeHelper.isEfactureEnable()) {
			return Tag.EVAL_BODY_INCLUDE;
		}

		return Tag.SKIP_BODY;
	}

}
