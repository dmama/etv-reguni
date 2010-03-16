package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * Ce tag permet d'afficher un bouton pour fermer l'overlay. Le bouton ne s'affiche que s'il est bien dans un overlay.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class JspTagCloseOverlayButton extends BodyTagSupport {

	private static final long serialVersionUID = -5339054043898758118L;

	private String text;

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public int doStartTag() throws JspTagException {
		try {
			JspWriter out = pageContext.getOut();

			final String libelleBouton = (text == null ? "Fermer" : text);

			out.print("<div id=\"closeOverlay\" style=\"float:right\">");
			out.print("<input id=\"closeOverlayButton\" type=\"button\" value=\"" + libelleBouton + "\" onClick=\"self.parent.tb_remove()\"/>");
			out.print("</div>");
			out.print("<script type=\"text/javascript\" language=\"Javascript1.3\">");
			out.print("    var isOverlay = (location.href != top.location.href);");
			out.print("    if (!isOverlay) {");
			out.print("        var button = E$('closeOverlay');");
			out.print("        button.style.display = 'none';");
			out.print("    }");
			out.print("</script>");

			// Skips the body.
			return SKIP_BODY;
		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}
}
