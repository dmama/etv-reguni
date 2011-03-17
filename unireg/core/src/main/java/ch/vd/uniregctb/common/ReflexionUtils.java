package ch.vd.uniregctb.common;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ReflexionUtils {

	/**
	 * @param clazz une classe quelconque
	 * @return la liste de toutes les annotations de cette classe (en incluant les annotations héritées)
	 */
	public static List<Annotation> getAllAnnotations(Class clazz) {
		List<Annotation> list = new ArrayList<Annotation>();
		while (clazz != null) {
			final Annotation[] as = clazz.getAnnotations();
			if (as != null) {
				list.addAll(Arrays.asList(as));
			}
			clazz = clazz.getSuperclass();
		}
		return list;
	}

	/**
	 * Détermine et retourne les descripteurs de propriétés d'une classe.
	 *
	 * @param clazz une classe quelconque
	 * @return la map <i>nom de propriété</i> => <i>descripteur</i> des propriétés trouvées sur la classe.
	 * @throws IntrospectionException en cas d'exception lors de l'introspection
	 */
	public static Map<String, PropertyDescriptor> getPropertyDescriptors(Class<?> clazz) throws IntrospectionException {

		BeanInfo info = Introspector.getBeanInfo(clazz);
		PropertyDescriptor[] descriptors = info.getPropertyDescriptors();
		HashMap<String, PropertyDescriptor> pds = new HashMap<String, PropertyDescriptor>();

		for (PropertyDescriptor descriptor : descriptors) {
			pds.put(descriptor.getName(), descriptor);
		}

		return pds;
	}
}
