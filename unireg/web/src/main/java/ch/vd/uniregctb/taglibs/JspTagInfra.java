package ch.vd.uniregctb.taglibs;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.util.HtmlUtils;

import ch.vd.registre.base.utils.ReadOnlyPropertyDescriptor;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

/**
 * Tag jsp qui permet de récupérer divers éléments du service infrastructure par id.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class JspTagInfra extends BodyTagSupport {

	private static final long serialVersionUID = -8958197495549589352L;

	// private final Logger LOGGER = Logger.getLogger(JspTagInfra.class);

	private String entityType;
	private Integer entityId;
	private String entityPropertyName;
	private String entityPropertyTitle;
	private static ServiceInfrastructureService service; // static -> hack pour obtenir le service infrastructure initialisé par spring dans le context d'appels jsp

	private static interface Invocator {
		Object invoke(ServiceInfrastructureService service, int id) throws Exception;
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
		public Object invoke(ServiceInfrastructureService service, int id) throws Exception {
			return method.invoke(service, id);
		}
	}

	private static final Map<String, Invocator> invocators = new HashMap<String, Invocator>();

	static {
		Class<ServiceInfrastructureService> clazz = ServiceInfrastructureService.class;
		try {
			invocators.put("canton", new MethodInvocator(clazz.getMethod("getCanton", int.class)));
			invocators.put("collectivite", new MethodInvocator(clazz.getMethod("getCollectivite", int.class)));
			invocators.put("localite", new MethodInvocator(clazz.getMethod("getLocaliteByONRP", int.class)));
			invocators.put("officeImpot", new MethodInvocator(clazz.getMethod("getOfficeImpot", int.class)));
			invocators.put("pays", new MethodInvocator(clazz.getMethod("getPays", int.class)));
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
			throw new RuntimeException("Le type d'entité '" + entityType + "' n'est pas connu du service infrastructure.");
		}

		Object property = null;
		Object title = null;
		try {
			Object entity = invocator.invoke(service, entityId);
			if (entity != null) {
				final ReadOnlyPropertyDescriptor displayDescriptor = new ReadOnlyPropertyDescriptor(entityPropertyName, entity.getClass());
				property = displayDescriptor.getReadMethod().invoke(entity);

				if (StringUtils.isNotBlank(entityPropertyTitle)) {
					final ReadOnlyPropertyDescriptor titleDescriptor = new ReadOnlyPropertyDescriptor(entityPropertyTitle, entity.getClass());
					title = titleDescriptor.getReadMethod().invoke(entity);
				}
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

		if (property != null) {
			try {
				final StringBuilder b = new StringBuilder();
				if (title != null) {
					b.append("<span title='").append(HtmlUtils.htmlEscape(title.toString())).append("'>");
				}
				b.append(HtmlUtils.htmlEscape(property.toString()));
				if (title != null) {
					b.append("</span>");
				}
				pageContext.getOut().print(b.toString());
			}
			catch (IOException e) {
				throw new JspException(e);
			}
		}
		return SKIP_BODY;
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

	public void setService(ServiceInfrastructureService service) {
		JspTagInfra.service = service;
	}
}
