package ch.vd.uniregctb.taglibs;

import ch.vd.uniregctb.utils.UniregModeHelper;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * permet d'inclure conditionnellement du code si l'application est en mode standalone.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class JspTagStandalone extends TagSupport {

	private static final long serialVersionUID = 1219771675377024087L;

	@Override
	public int doStartTag() throws JspTagException {
		if (UniregModeHelper.isStandalone()) {
			return Tag.EVAL_BODY_INCLUDE;
		}

		return Tag.SKIP_BODY;
	}
}