package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import ch.vd.uniregctb.utils.EnvironnementHelper;
import ch.vd.uniregctb.web.HttpUtilities;

/**
 * Tag qui va chercher dans les propriétés de l'application
 * le nom de l'environnement courant
 */
public class JspTagEnvironnement extends BodyTagSupport {

	@Override
	public int doStartTag() throws JspTagException {
		try {
			final JspWriter out = pageContext.getOut();

			out.print(HttpUtilities.htmlEncode(EnvironnementHelper.getEnvironnement()));

			// Skips the body.
			return SKIP_BODY;
		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}

}
