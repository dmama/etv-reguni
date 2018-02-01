package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import ch.vd.uniregctb.common.AuthenticationHelper;

public class JspTagUser extends BodyTagSupport {

	private static final long serialVersionUID = -8929L;

	@Override
	public int doStartTag() throws JspTagException {
		try {
			JspWriter out = pageContext.getOut();

			if (AuthenticationHelper.isAuthenticated()) {
				String prenom = AuthenticationHelper.getFirstName();
				String nom = AuthenticationHelper.getLastName();
				String oid = AuthenticationHelper.getCurrentOIDSigle();
				out.print(buidHtlm(nom, prenom, oid));
			}
			// Skips the body.
			return SKIP_BODY;
		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}

	private String buidHtlm(String nom, String prenom, String oid) {
		StringBuilder str = new StringBuilder();

		if (prenom != null) {
			str.append(prenom);
		}

		if (nom != null) {
			str.append(' ');
			str.append(nom);
		}

		if (oid != null) {
			str.append(" (");
			str.append(oid);
			str.append(')');
		}

		return str.toString();
	}

}
