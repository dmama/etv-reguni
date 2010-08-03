package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * Ce tag permet d'afficher un bouton pour fermer l'overlay. Le bouton ne s'affiche que s'il est bien dans un overlay.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class JspTagRetourButton extends BodyTagSupport {

	private static final long serialVersionUID = -5339054043898795318L;

	private String text;
	private String link;



	public void setText(String text) {
		this.text = text;
	}

	public void setLink(String link) {
		this.link = link;
	}


	@Override
	public int doStartTag() throws JspTagException {
		try {
			JspWriter out = pageContext.getOut();

			final String libelleBouton = (text == null ? "Retour" : text);


			out.print("<input id=\"retourButton\" type=\"button\" value=\"" + libelleBouton + "\" onClick=\"javascript:Page_RetourToVisualisation('"+link+"');\"/>");
			out.print("<script type=\"text/javascript\" language=\"Javascript1.3\">");
			out.print("function Page_RetourToVisualisation(lien) {");		
			out.print("    if(confirm(\"Voulez-vous vraiment quitter cette page sans sauver le tiers ?\")) {");
			out.print("        document.location.href=lien;");
			out.print("  }");
			out.print(" }");	
			out.print("</script>");





			// Skips the body.
			return SKIP_BODY;
		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}
}