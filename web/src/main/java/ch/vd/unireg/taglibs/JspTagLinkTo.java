package ch.vd.unireg.taglibs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.web.servlet.tags.RequestContextAwareTag;
import org.springframework.web.util.HtmlUtils;

/**
 * Button qui navigue vers ou qui poste un formulaire sur autre URL.
 */
public class JspTagLinkTo extends RequestContextAwareTag {

	private static final Logger LOGGER = LoggerFactory.getLogger(JspTagLinkTo.class);

	private static final long serialVersionUID = -8876431425915581399L;

	private String name;
	private String action;
	private String params;
	private String method = "get";
	private String confirm;
	private String title;
	private String link_class = "link_to";

	private String contextPath;
	private MessageSource messageSource;

	@Override
	public int doStartTagInternal() throws JspException {

		final HttpServletRequest request = (HttpServletRequest) this.pageContext.getRequest();
		contextPath = request.getContextPath();
		messageSource = getRequestContext().getMessageSource();

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

	public void setName(String name) {
		this.name = name;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public void setParams(String params) {
		this.params = params;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public void setConfirm(String confirm) {
		this.confirm = confirm;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setLink_class(String link_class) {
		this.link_class = link_class;
	}

	private String buildHtml() {

		String confirmScript = null;
		if (StringUtils.isNotBlank(confirm)) {
			String s = StringEscapeUtils.escapeEcmaScript(resolveCode(confirm));
			s = s.replaceAll("\\\\n", "\\n"); // on un-escape les retours de lignes, parce qu'ils sont bien pratiques
			confirmScript = "if (!confirm('" + s + "')) return false;";
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
			sb.append(" title=\"").append(HtmlUtils.htmlEscape(resolveCode(title))).append("\"");
		}
		if (onclickScript != null) {
			sb.append(" onclick=\"").append(onclickScript).append("\"");
		}
		sb.append(">");
		if (StringUtils.isBlank(name)) {
			sb.append("&nbsp;"); // compatibilité IE8
		}
		else {
			sb.append(HtmlUtils.htmlEscape(resolveCode(name)));
		}
		sb.append("</a>");

		return sb.toString();
	}

	private String resolveCode(String code) {
		final String resolvedName;
		if (StringUtils.isNotBlank(code)) {
			resolvedName = messageSource.getMessage(code, null, getRequestContext().getLocale());
		}
		else {
			resolvedName = code;
		}
		return resolvedName;
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
		return "Form.dynamicSubmit('" + method + "','" + contextPath + action + "'," + normalizeParam(params) + "); return false;";
	}

	/**
	 * [SIFISC-7865] Décode et réencode les paramètres reçus de telle manière que le format respecte la norme JSON stricte (= toutes les clés sont entourées de guillemets). Autrement, IE8 pleure parce
	 * qu'il ne comprend la syntaxe des paramètres.
	 *
	 * @param params les paramètres sous forme JSON permissif
	 * @return les paramètres sous forme JSON stricte
	 */
	private static String normalizeParam(String params) {
		final StringBuilder s = new StringBuilder();
		s.append('{');
		if (StringUtils.isNotBlank(params)) {
			Map<String, String> paramsMap = parseParams(params);
			for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
				if (s.length() > 1) {
					s.append(",");
				}
				s.append('\'').append(entry.getKey()).append('\'').append(":");
				s.append('\'').append(entry.getValue()).append('\'');
			}
		}
		s.append('}');
		return s.toString();
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
			final Map<String, String> map = new HashMap<>();
			JsonParser jParser = jfactory.createParser(params);
			jParser.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
			jParser.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
			while (jParser.nextToken() != JsonToken.END_OBJECT) {
				if (jParser.getCurrentToken() != JsonToken.START_OBJECT) {
					map.put(jParser.getCurrentName(), jParser.getText());
				}
			}
			return map;
		}
		catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
			return Collections.emptyMap();
		}
	}
}

