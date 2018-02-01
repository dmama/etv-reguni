package ch.vd.unireg.taglibs;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.taglibs.standard.tag.common.core.Util;

import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.tiers.manager.AutorisationCache;
import ch.vd.unireg.tiers.manager.Autorisations;

@SuppressWarnings("UnusedDeclaration")
public class JspTagSetAutorisations extends BodyTagSupport {

	private String var;
	private Long tiersId;
	private int scope = PageContext.PAGE_SCOPE;

	private static AutorisationCache cache;

	@Override
	public int doEndTag() throws JspException {
		final Autorisations autorisations = cache.getAutorisations(tiersId, AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID());
		pageContext.setAttribute(var, autorisations, scope);
		return EVAL_PAGE;
	}

	public void setVar(String var) {
		this.var = var;
	}

	public void setTiersId(Long tiersId) {
		this.tiersId = tiersId;
	}

	public void setScope(String scope) {
		this.scope = Util.getScope(scope);
	}

	public void setCache(AutorisationCache cache) {
		JspTagSetAutorisations.cache = cache;
	}
}
