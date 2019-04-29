package ch.vd.unireg.taglibs;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.HtmlUtils;

import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;

/**
 * Tag jsp qui permet de récupérer divers éléments du service infrastructure par id.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class JspTagInfra extends BodyTagSupport {

	private static final long serialVersionUID = -8958197495549589352L;

	private final Logger LOGGER = LoggerFactory.getLogger(JspTagInfra.class);

	private String entityType;
	private Integer entityId;
	private String entityPropertyName;
	private String entityPropertyTitle;
	private EscapeMode escapeMode = EscapeMode.HTML;
	private static ServiceInfrastructureService service; // static -> hack pour obtenir le service infrastructure initialisé par spring dans le context d'appels jsp

	private interface Invocator {
		Object invoke(ServiceInfrastructureService service, int id) throws InvocationTargetException, IllegalAccessException;
	}

	/**
	 * Invoke une méthode avec un paramètre entier
	 */
	private static class MethodInvocator implements Invocator {
		private final Method method;
		public MethodInvocator(Method method) {
			this.method = method;
		}
		@Override
		public Object invoke(ServiceInfrastructureService service, int id) throws InvocationTargetException, IllegalAccessException {
			return method.invoke(service, id);
		}
	}

	private static final Map<String, Invocator> invocators = new HashMap<>();

	static {
		Class<ServiceInfrastructureService> clazz = ServiceInfrastructureService.class;
		try {
			invocators.put("canton", new MethodInvocator(clazz.getMethod("getCanton", int.class)));
			invocators.put("collectivite", new MethodInvocator(clazz.getMethod("getCollectivite", int.class)));
			invocators.put("officeImpot", new MethodInvocator(clazz.getMethod("getOfficeImpot", int.class)));
			invocators.put("rue", new MethodInvocator(clazz.getMethod("getRueByNumero", int.class)));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int doStartTag() throws JspException {

		final Invocator invocator = invocators.get(entityType);
		if (invocator == null) {
			throw new JspException("Le type d'entité '" + entityType + "' n'est pas connu du service infrastructure.");
		}

		Object property = null;
		Object title = null;
		try {
			Object entity = invocator.invoke(service, entityId);
			if (entity != null) {
				property = PropertyUtils.getProperty(entity, entityPropertyName);

				if (StringUtils.isNotBlank(entityPropertyTitle)) {
					title = PropertyUtils.getProperty(entity, entityPropertyTitle);
				}
			}

			if (property != null) {
				final StringBuilder b = new StringBuilder();
				if (title != null) {
					b.append("<span title='").append(HtmlUtils.htmlEscape(title.toString())).append("'>");
				}
				b.append(escapeMode.escape(property.toString()));
				if (title != null) {
					b.append("</span>");
				}
				print(b.toString());
			}
		}
		catch (Exception e) {
			// [SIFISC-5427] le mécanisme d'interception des exceptions (voir le bean 'urlMappingExceptionResolver') ne fonctionne pas dans le context
			// des tags JSP, on doit donc directement afficher un message d'erreur pour signaler le problème.
			LOGGER.error(e.getMessage(), e);
			print("<span class=\"error\">##Exception : " + e.getMessage() + "##</span>");
		}
		return SKIP_BODY;
	}

	private void print(String b) throws JspException {
		try {
			pageContext.getOut().print(b);
		}
		catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
			throw new JspException(e);
		}
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public void setEntityId(Integer entityId) {
		this.entityId = entityId;
	}

	public void setEntityPropertyName(String entityPropertyName) {
		this.entityPropertyName = entityPropertyName;
	}

	public void setEntityPropertyTitle(String entityPropertyTitle) {
		this.entityPropertyTitle = entityPropertyTitle;
	}

	public void setEscapeMode(String escapeMode) {
		this.escapeMode = EscapeMode.fromString(escapeMode);
		if (this.escapeMode == null) {
			this.escapeMode = EscapeMode.HTML;
		}
	}

	public void setService(ServiceInfrastructureService service) {
		JspTagInfra.service = service;
	}
}
