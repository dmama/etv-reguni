package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.security.Portail;

/**
 * Ce tag permet d'inclure conditionnellement un fragment de code JSP en fonction du portail d'accès (IAM ou CYBER) utilisé par l'utilisateur courant pour accéder à Unireg.
 */
public class JspTagIfPortail extends TagSupport {

	private static final long serialVersionUID = -5832005163589177589L;

	private Portail portail;

	@Override
	public int doStartTag() throws JspTagException {
		if (AuthenticationHelper.getAccessPortail() == portail) {
			return Tag.EVAL_BODY_INCLUDE;
		}
		return Tag.SKIP_BODY;
	}

	public void setPortail(Portail portail) {
		this.portail = portail;
	}
}
