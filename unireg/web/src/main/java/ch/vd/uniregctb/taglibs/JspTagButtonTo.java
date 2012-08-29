package ch.vd.uniregctb.taglibs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

/**
 * Button qui navigue vers ou qui poste un formulaire sur autre URL.
 */
@SuppressWarnings("UnusedDeclaration")
public class JspTagButtonTo extends BodyTagSupport {

	private static final Logger LOGGER = Logger.getLogger(JspTagButtonTo.class);

	private static final long serialVersionUID = 4115565970912710828L;

	private String name;
	private String action;
	private String params;
	private String method = "post";
	private String confirm;
	private String form_class = "button_to";
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

	public String getForm_class() {
		return form_class;
	}

	public void setForm_class(String form_class) {
		this.form_class = form_class;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public String buildHtml() {
		final StringBuilder sb = new StringBuilder();
		sb.append("<form method=\"").append(method).append("\" action=\"").append(contextPath).append(action).append("\" class=\"").append(form_class).append("\">");
		sb.append("<div>");
		if (StringUtils.isNotBlank(params)) {
			Map<String, String> paramsMap = parseParams(params);
			for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
				sb.append("<input type=\"hidden\" name=\"").append(entry.getKey()).append("\" value=\"").append(entry.getValue()).append("\"/>");
			}
		}
		sb.append("<input value=\"").append(name).append("\" type=\"submit\"");
		if (StringUtils.isNotBlank(confirm)) {
			sb.append(" onclick=\"if (!confirm('").append(StringEscapeUtils.escapeJavaScript(confirm)).append("')) return false;\"");
		}
		if (disabled) {
			sb.append(" disabled=\"disabled\"");
		}
		sb.append("/></div></form>");
		return sb.toString();
	}

	/**
	 * Parse la structure JSON reçue en paramètre (on suppose qu'il s'agit d'une structure simple, non-hiérarchique) et retourne la map des clés-valeurs correspondante.
	 *
	 * @param params des paramètres sous format JSON
	 * @return une map clé-valeur des paramètres.
	 */
	private static Map<String, String> parseParams(String params) {
		JsonFactory jfactory = new JsonFactory();
		try {
			final Map<String, String> map = new HashMap<String, String>();
			JsonParser jParser = jfactory.createJsonParser(params);
			while (jParser.nextToken() != JsonToken.END_OBJECT) {
				if (jParser.getCurrentToken() != JsonToken.START_OBJECT) {
					map.put(jParser.getCurrentName(), jParser.getText());
				}
			}
			return map;
		}
		catch (IOException e) {
			LOGGER.error(e);
			return Collections.emptyMap();
		}
	}
}

