package ch.vd.uniregctb.taglibs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import ch.vd.uniregctb.security.IFOSecAuthenticationProcessingFilter;
import ch.vd.uniregctb.security.UniregIAMAuthProcessingFilter;

public class JspTagUser extends BodyTagSupport {

	private static final long serialVersionUID = -8929L;

	@Override
	public int doStartTag() throws JspTagException {
		try {
			JspWriter out = pageContext.getOut();

			HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
			HttpSession session = request.getSession(false);
			if (session != null) {
				String prenom = (String) session.getAttribute(UniregIAMAuthProcessingFilter.UNIREG_IAM_FIRST);
				String nom = (String)session.getAttribute(UniregIAMAuthProcessingFilter.UNIREG_IAM_LAST);
				String oid = (String) session.getAttribute(IFOSecAuthenticationProcessingFilter.USER_OID_SIGLE);
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
			str.append(" ");
			str.append(nom);
		}

		if (oid != null) {
			str.append(" (");
			str.append(oid);
			str.append(")");
		}

		return str.toString();
	}

}
