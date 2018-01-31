package ch.vd.uniregctb.taglibs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import ch.vd.uniregctb.common.FormatNumeroHelper;

public class JspTagNoCTB extends BodyTagSupport {

	private static final long serialVersionUID = -8926948881185525899L;

	private Long numero;
	private boolean link;

	@Override
	public int doStartTag() throws JspTagException {
		try {
			JspWriter out = pageContext.getOut();
			out.print(buidHtlm());
			// Skips the body.
			return SKIP_BODY;
		}
		catch (Exception ex) {
			throw new JspTagException(ex);
		}
	}

	public void setNumero(Long num) {
		this.numero = num;
	}

	public void setLink(boolean link) {
		this.link = link;
	}

	private String buidHtlm() {

		final StringBuilder sb = new StringBuilder();

		if (link) {
			final HttpServletRequest request = (HttpServletRequest) this.pageContext.getRequest();
			final String contextPath = request.getContextPath();

			sb.append("<a href=\"").append(contextPath).append("/tiers/visu.do?id=").append(numero).append("\">");
		}

		sb.append(FormatNumeroHelper.numeroCTBToDisplay(numero));

		if (link) {
			sb.append("</a>");
		}

		return sb.toString();
	}

}


