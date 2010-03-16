package ch.vd.uniregctb.taglibs;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

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
	private static ServiceInfrastructureService service; // static -> hack pour obtenir le service infrastructure initialisé par spring dans
	// le context d'appels jsp

	private static Map<String, Method> getters = new HashMap<String, Method>();

	static {
		Class<ServiceInfrastructureService> clazz = ServiceInfrastructureService.class;
		try {
			getters.put("canton", clazz.getMethod("getCanton", int.class));
			getters.put("collectivite", clazz.getMethod("getCollectivite", int.class));
			getters.put("commune", clazz.getMethod("getCommuneByNumeroOfsEtendu", int.class));
			getters.put("localite", clazz.getMethod("getLocaliteByONRP", int.class));
			getters.put("officeImpot", clazz.getMethod("getOfficeImpot", int.class));
			getters.put("pays", clazz.getMethod("getPays", int.class));
			getters.put("rue", clazz.getMethod("getRueByNumero", int.class));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int doStartTag() throws JspException {

		final Method getter = getters.get(entityType);
		if (getter == null) {
			throw new RuntimeException("Le type d'entité '" + entityType + "' n'est pas connu du service infrastructure.");
		}

		Object property = null;
		try {
			Object entity = getter.invoke(service, entityId.intValue());
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
