package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.commons.lang3.StringUtils;

/**
 * Ce tag permet d'afficher un bouton pour quitter une page d'édition sans sauvegarder les modifications en cours. Il peut être configuré pour
 * afficher un message de confirmation à l'utilisateur.
 */
public class JspTagRetourButton extends BodyTagSupport {

	private static final long serialVersionUID = -5339054043898795318L;

	private String text;
	private String link;
	private String message;
	private boolean checkIfModified;
	private Integer tabIndex;

	public void setText(String text) {
		this.text = text;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setCheckIfModified(boolean checkIfModified) {
		this.checkIfModified = checkIfModified;
	}

	public void setTabIndex(Integer tabIndex) {
		this.tabIndex = tabIndex;
	}

	@Override
	public int doStartTag() throws JspTagException {
		try {
			final JspWriter out = pageContext.getOut();
			final String libelleBouton = (text == null ? "Retour" : text);
			final String libelleMessage = (message == null ? "Voulez-vous vraiment quitter cette page sans sauver le tiers ?" : message);
			final String tabIndexStr = (tabIndex == null ? StringUtils.EMPTY : " tabIndex=\"" + tabIndex + "\"");

			out.print("<input id=\"retourButton\" type=\"button\"" + tabIndexStr + " value=\"" + libelleBouton + "\" onClick=\"javascript:Page_RetourToVisualisation('" + link + "','" + libelleMessage + "');\"/>");
			out.print("<script type=\"text/javascript\" language=\"Javascript1.3\">");
			out.print("function Page_RetourToVisualisation(lien,message) {");

			if (checkIfModified) {
				out.print("    if (!Modifier.isModified || confirm(message)) {");
			}
			else {
				out.print("    if (confirm(message)) {");
			}
			//out.print("        document.getElementById('retourButton').disabled = true;");
			out.print(" var inputs = document.getElementsByTagName('INPUT');");
			out.print(" for (var i = 0; i < inputs.length; i++) {");
			out.print("     inputs[i].disabled = true;");
			out.print(" }");
			out.print(" document.location.href = lien;");
			out.print("    }");
			out.print(" }");
			out.print("</script>");

			return SKIP_BODY;
		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}
}