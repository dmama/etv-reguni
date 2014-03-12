package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.springframework.web.util.HtmlUtils;

import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.uniregctb.common.HtmlHelper;

/**
 * Ce tag permet d'afficher une call-stack résultant d'une exception de manière minimale : la call-stack est cachée par défaut et
 * l'utilisateur peut cliquer sur un lieu pour l'afficher.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class JspTagCallstack extends BodyTagSupport {

	private static final long serialVersionUID = -5337549995900594216L;

	private String headerMessage;
	private Exception exception;
	private String callstack;

	@Override
	public int doStartTag() throws JspTagException {
		try {
			JspWriter out = pageContext.getOut();

			// la table qui contient la callstack
			out.print("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\">");
			out.print("<tr><td>");
			out.print(HtmlUtils.htmlEscape(headerMessage));
			out.print("<span id=\"details_link\">(<a href=\"#\" onclick=\"javascript:showDetails()\">détails</a>)</span></td></tr>");
			out.print("<tr id=\"exception_callstack\"><td class=\"callstack\">");
			final String cs = (exception != null ? ExceptionUtils.extractCallStack(exception) : callstack);
			out.print(HtmlHelper.renderMultilines(cs));
			out.print("</td></tr>");
			out.print("</table>");

			// le code javascript pour cacher la callstack par défaut
			out.print("<script type=\"text/javascript\">");
			out.print("$('#exception_callstack').hide();");
			out.print("function showDetails() {");
			out.print("  $('#exception_callstack').show();");
			out.print("  $('#details_link').hide();");
			out.print("}");
			out.print("</script>");

			// Skips the body.
			return SKIP_BODY;
		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}

	public void setHeaderMessage(String headerMessage) {
		this.headerMessage = headerMessage;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}

	public void setCallstack(String callstack) {
		this.callstack = callstack;
	}
}
