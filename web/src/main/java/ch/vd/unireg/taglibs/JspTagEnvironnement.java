package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.commons.lang.StringEscapeUtils;

import ch.vd.uniregctb.utils.UniregModeHelper;

/**
 * Tag qui va chercher dans les propriétés de l'application
 * le nom de l'environnement courant
 */
public class JspTagEnvironnement extends BodyTagSupport {

	@Override
	public int doStartTag() throws JspTagException {
		try {
			final JspWriter out = pageContext.getOut();

			out.print(StringEscapeUtils.escapeHtml(UniregModeHelper.getEnvironnement()));

			// Skips the body.
			return SKIP_BODY;
		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}

}
