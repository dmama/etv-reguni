package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import java.util.Date;

import org.springframework.web.util.HtmlUtils;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

/**
 * Button qui ouvre une boîte de dialogue pour la consultation des information de traitements du messsage  d'identification
 */
public class JspTagConsulterInfoTraitement extends BodyTagSupport {

	private static final long serialVersionUID = -3555171446830251592L;

	private Date dateTraitement;
	private String userTraitement;

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

	public Date getDateTraitement() {
		return dateTraitement;
	}

	public void setDateTraitement(Date dateTraitement) {
		this.dateTraitement = dateTraitement;
	}

	public String getUserTraitement() {
		return userTraitement;
	}

	public void setUserTraitement(String userTraitement) {
		this.userTraitement = userTraitement;
	}

	public String buildHtml() {
		String stringDate = DateHelper.dateTimeToDisplayString(dateTraitement);
		String stringUser = HtmlUtils.htmlEscape(userTraitement);
		String onclick = "return open_consulter_info_traitement('" + stringUser + "', '" + stringDate + "');";
		return String.format("<a href=\"#\" class=\"infotraitement\" title=\"Consultation des infos de traitement\" onclick=\"%s\">&nbsp;</a>", onclick);

	}
}

