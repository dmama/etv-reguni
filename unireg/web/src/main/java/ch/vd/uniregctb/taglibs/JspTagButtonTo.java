package ch.vd.uniregctb.taglibs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.web.util.HtmlUtils;

/**
 * Button qui navigue vers ou qui poste un formulaire sur autre URL.
 */
@SuppressWarnings("UnusedDeclaration")
public class JspTagButtonTo extends BodyTagSupport {

	private static final Logger LOGGER = Logger.getLogger(JspTagButtonTo.class);

	private static final long serialVersionUID = 4115565970912710828L;

	private String id;
	private String name;
	private String action;
	private String params;
	private String method = "post";
	private String confirm;
	private String title;
	private String button_class = "button_to";
	private boolean visible = true;
	private boolean disabled = false;

	private String contextPath;

	@Override
	public int doStartTag() throws JspException {

		final HttpServletRequest request = (HttpServletRequest) this.pageContext.getRequest();
		contextPath = request.getContextPath();

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

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getParams() {
		return params;
	}

	public void setParams(String params) {
		this.params = params;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getConfirm() {
		return confirm;
	}

	public void setConfirm(String confirm) {
		this.confirm = confirm;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getButton_class() {
		return button_class;
	}

	public void setButton_class(String button_class) {
		this.button_class = button_class;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public String buildHtml() {

		final StringBuilder onclickScript = new StringBuilder();

		if (StringUtils.isNotBlank(confirm)) {
			final String confirmScript = "if (!confirm('" + StringEscapeUtils.escapeJavaScript(confirm) + "')) return false;";
			onclickScript.append(confirmScript);
		}

		if ("get".equalsIgnoreCase(method)) {
			onclickScript.append("window.location.href='").append(JspTagLinkTo.buildGetUrl(contextPath, action, params)).append("'; return false");
		}
		else {
			final String submitForm = JspTagLinkTo.buildSubmitFormScript(contextPath, action, method, params);
			onclickScript.append(submitForm);
		}

		final StringBuilder sb = new StringBuilder();
		sb.append("<input");
		if (StringUtils.isNotBlank(id)) {
			sb.append(" id=\"").append(id).append("\"");
		}
		if (!visible) {
			sb.append(" style=\"display:none;\"");
		}
		sb.append(" type=\"button\" value=\"").append(HtmlUtils.htmlEscape(name)).append("\" class=\"").append(button_class).append("\"");
		if (StringUtils.isNotBlank(title)) {
			sb.append(" title=\"").append(HtmlUtils.htmlEscape((title))).append("\"");
		}
		sb.append(" onclick=\"").append(onclickScript).append("\"");
		if (disabled) {
			sb.append(" disabled=\"disabled\"");
		}
		sb.append("/>");

		return sb.toString();
	}
}

