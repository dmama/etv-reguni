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
import org.springframework.web.util.HtmlUtils;

/**
 * Button qui navigue vers ou qui poste un formulaire sur autre URL.
 */
@SuppressWarnings("UnusedDeclaration")
public class JspTagLinkTo extends BodyTagSupport {

	private static final Logger LOGGER = Logger.getLogger(JspTagLinkTo.class);

	private static final long serialVersionUID = 4115565970912710828L;

	private String name;
	private String action;
	private String params;
	private String method = "get";
	private String confirm;
	private String title;
	private String link_class = "link_to";

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

	public void setTitle(String title) {
		this.title = title;
	}

	public String getLink_class() {
		return link_class;
	}

	public void setLink_class(String link_class) {
		this.link_class = link_class;
	}

	public String buildHtml() {

		String confirmScript = null;
		if (StringUtils.isNotBlank(confirm)) {
			confirmScript = "if (!confirm('" + StringEscapeUtils.escapeJavaScript(confirm) + "')) return false;";
		}

		final String url;
		final String onclickScript;

		if ("get".equalsIgnoreCase(method)) {
			url = buildGetUrl(contextPath, action, params);
			onclickScript = confirmScript;
		}
		else {
			final String submitForm = buildSubmitFormScript(contextPath, action, method, params);
			url = contextPath + action;

			if (confirmScript != null) {
				onclickScript = confirmScript + submitForm;
			}
			else {
				onclickScript = submitForm;
			}
		}

		final StringBuilder sb = new StringBuilder();
		sb.append("<a href=\"").append(url).append("\" class=\"").append(link_class).append("\"");
		if (StringUtils.isNotBlank(title)) {
			sb.append(" title=\"").append(HtmlUtils.htmlEscape((title))).append("\"");
		}
		if (onclickScript != null) {
			sb.append(" onclick=\"").append(onclickScript).append("\"");
		}
		sb.append(">");
		if (StringUtils.isBlank(name)) {
			sb.append("&nbsp;"); // compatibilité IE6
		}
		else {
			sb.append(StringEscapeUtils.escapeJavaScript(name));
		}
		sb.append("</a>");

		return sb.toString();
	}

	/**
	 * Construit le javascript qui permet de créer dynamiquement et de soumettre un formulaire avec l'action et les paramètres spécifiés.
	 *
	 * @param contextPath le context path de l'application
	 * @param action      l'action (= l'url sans paramètre, e.g. "/tiers/visu.do")
	 * @param method      la méthode de soumission du formulaire ("get", "post", ...)
	 * @param params      les paramètres au format JSON sans guillement (e.g. "{id:3}")
	 * @return le code javascript qui va bien.
	 */
	public static String buildSubmitFormScript(String contextPath, String action, String method, String params) {
		return "Form.dynamicSubmit('" + method + "', '" + contextPath + action + "', " + params + "); return false;";
	}

	/**
	 * Construit une url complète et valide en fonction de l'action et des paramètres spécifiés.
	 *
	 * @param contextPath le context path de l'application
	 * @param action      l'action (= l'url sans paramètre, e.g. "/tiers/visu.do")
	 * @param params      les paramètres au format JSON sans guillement (e.g. "{id:3}")
	 * @return l'url complète (e.g. "/fiscalite/unireg/web/tiers/visu.do?id=3")
	 */
	public static String buildGetUrl(String contextPath, String action, String params) {
		final StringBuilder url = new StringBuilder();
		if (StringUtils.isNotBlank(params)) {
			Map<String, String> paramsMap = parseParams(params);
			for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
				if (url.length() > 0) {
					url.append("&");
				}
				url.append(entry.getKey()).append("=").append(entry.getValue());
			}
		}
		if (url.length() > 0) {
			url.insert(0, "?");
		}
		url.insert(0, action).insert(0, contextPath);
		return url.toString();
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
			jParser.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
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

