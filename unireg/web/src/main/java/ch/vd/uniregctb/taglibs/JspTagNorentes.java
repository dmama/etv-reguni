package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

import ch.vd.uniregctb.norentes.common.NorentesFactory;
import ch.vd.uniregctb.norentes.common.NorentesManager;

/**
 * permet d'inclure conditionnellement du code si l'application est en mode Norentes.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class JspTagNorentes extends TagSupport {

	private static final long serialVersionUID = 8500507254282939898L;

	@Override
	public int doStartTag() throws JspTagException {
		final NorentesManager norentesManager = NorentesFactory.getNorentesManager();
		if (norentesManager.isActif()) {
			return Tag.EVAL_BODY_INCLUDE;
		}

		return Tag.SKIP_BODY;
	}
}
