package ch.vd.uniregctb.taglibs;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import ch.vd.registre.base.utils.ReadOnlyPropertyDescriptor;
import ch.vd.uniregctb.interfaces.model.Commune;
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
		public Object invoke(ServiceInfrastructureService service, int id) throws Exception {
			return method.invoke(service, id);
		}
	}

	/**
	 * Appelle la méthode {@link ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService#getCommuneByNumeroOfsEtendu(int, ch.vd.registre.base.date.RegDate)}
	 * avec la date à <code>null</code>
	 */
	private static class CommuneInvocator implements Invocator {
		public Commune invoke(ServiceInfrastructureService service, int id) throws Exception {
			return service.getCommuneByNumeroOfsEtendu(id, null);
		}
	}

	private static Map<String, Invocator> invocators = new HashMap<String, Invocator>();

	static {
		Class<ServiceInfrastructureService> clazz = ServiceInfrastructureService.class;
		try {
			invocators.put("canton", new MethodInvocator(clazz.getMethod("getCanton", int.class)));
			invocators.put("collectivite", new MethodInvocator(clazz.getMethod("getCollectivite", int.class)));
			invocators.put("commune", new CommuneInvocator());
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
		try {
			Object entity = invocator.invoke(service, entityId);
			if (entity != null) {
				ReadOnlyPropertyDescriptor descriptor = new ReadOnlyPropertyDescriptor(entityPropertyName, entity.getClass());
				property = descriptor.getReadMethod().invoke(entity);
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

		if (property != null) {
			try {
				pageContext.getOut().print(property.toString());
			}
			catch (IOException e) {
				throw new JspException(e);
			}
		}
		return SKIP_BODY;
	}

	public static String capitalize(String name) {
		if (name == null || name.length() == 0) {
			return name;
		}
		return name.substring(0, 1).toUpperCase() + name.substring(1);
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

	public void setService(ServiceInfrastructureService service) {
		JspTagInfra.service = service;
	}
}
