package ch.vd.unireg.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.util.Date;

import org.apache.commons.lang.StringEscapeUtils;

import ch.vd.registre.base.date.DateConstants;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDateHelper;

/**
 * [UNIREG-2824] Ce tag permet d'afficher un message d'avertissement lorsqu'un offset est spécifié sur la date courante.
 */
public class JspTagDateOffset extends BodyTagSupport {

	@Override
	public int doStartTag() throws JspTagException {
		try {
			final JspWriter out = pageContext.getOut();

			if (DateConstants.TIME_OFFSET != 0) {
				final Date now = DateHelper.getCurrentDate();
				final String message = "Attention ! Pour le testing, la date courante de l'application est décalée de " + DateHelper.getTimeOffsetAsString(DateConstants.TIME_OFFSET, false) +
						". La jour courant est donc le " + RegDateHelper.dateToDisplayString(RegDateHelper.get(now)) + ", et l'heure " + DateHelper.timeToString(now) + '.';
				out.print("<div class=\"flash-error\">" + StringEscapeUtils.escapeHtml(message) + "</div>");
			}

			// Skips the body.
			return SKIP_BODY;
		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}
}