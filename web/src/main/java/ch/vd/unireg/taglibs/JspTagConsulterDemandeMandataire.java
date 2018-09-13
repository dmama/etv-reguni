package ch.vd.unireg.taglibs;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * Button qui ouvre une boîte de dialogue pour la consultation des détails d'une demande de délai faite par un mandataire.
 */
public class JspTagConsulterDemandeMandataire extends BodyTagSupport {

	private static final long serialVersionUID = -3555171446896251592L;

	private String demandeId;

	@Override
	public int doStartTag() throws JspException {
		try {
			final JspWriter out = pageContext.getOut();
			out.print(buildHtml());
			// Skips the body.
			return SKIP_BODY;
		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}

	@SuppressWarnings("unused")
	public void setDemandeId(String demandeId) {
		this.demandeId = demandeId;
	}

	public String buildHtml() {
		return String.format("<a href=\"#\" class=\"consult_mandataire\" title=\"Délai demandé par un mandataire\" onclick=\"return Dialog.open_demande_mandataire(%s);\">&nbsp;</a>", demandeId);
	}
}
