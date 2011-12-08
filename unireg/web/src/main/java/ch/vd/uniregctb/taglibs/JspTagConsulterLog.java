package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * Button qui ouvre une boîte de dialogue pour la consultation des logs
 */
public class JspTagConsulterLog extends BodyTagSupport {

	private static final long serialVersionUID = -3555171446896251592L;

	private String entityNature;
	private String entityId;

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

	@SuppressWarnings({"UnusedDeclaration"})
	public String getEntityNature() {
		return entityNature;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEntityNature(String title) {
		this.entityNature = title;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public String getEntityId() {
		return entityId;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public String buildHtml() {
		return String.format("<a href=\"#\" class=\"consult\" title=\"Consultation des logs\" onclick=\"return open_consulter_log('%s', %s);\">&nbsp;</a>", entityNature, entityId);
	}
}

