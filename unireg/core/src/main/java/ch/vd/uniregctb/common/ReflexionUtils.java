package ch.vd.uniregctb.common;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

	/**
	 * Converti un objet en une string. Cette méthode utilise la réflexion pour découvrir les propriétés de l'objet.
	 * <p/>
	 * <b>Exemple:</b> <pre>GetTiersHisto{login=UserLogin{oid=22, userId="PerfsClient"}, parts=[ADRESSES, ADRESSES_ENVOI], tiersNumber=10722347}</pre>
	 *
	 * @param o        un objet, qui peut être nul.
	 * @param showNull <b>vrai</b> si les propriétés nulles (et les collections vides) doivent apparaîtrent dans la string; <b>faux</b> si elles doivent être ignorées.
	 * @return la représentation string de l'objet spécifié.
	 */
	public static String toString(Object o, boolean showNull) {
		return toString(o, showNull, new HashSet<Object>());
	}

	private static String toString(Object o, boolean showNull, Set<Object> processed) {
		if (o == null) {
			return "null";
		}

		// Cas triviaux
		if (o instanceof Collection) {
			return toString((Collection<?>) o, showNull, processed);
		}
		else if (o instanceof Number || o instanceof Boolean || o instanceof Enum || o instanceof Character) {
			return o.toString();
		}
		else if (o instanceof String) {
			return String.format("\"%s\"", o);
		}
		else if (o instanceof Class) {
			return ((Class) o).getName();
		}

		// on évite de partir en récursion infinie
		if (processed.contains(o)) {
			return "<...>";
		}
		processed.add(o);

		// Cas généraux : on passe par la réflexion java
		try {
			final Map<String, PropertyDescriptor> descriptors = getPropertyDescriptors(o.getClass());
			final List<PropertyDescriptor> list = new ArrayList<PropertyDescriptor>(descriptors.values());
			Collections.sort(list, new Comparator<PropertyDescriptor>() {
				@Override
				public int compare(PropertyDescriptor o1, PropertyDescriptor o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});

			final StringBuilder s = new StringBuilder();
			s.append(o.getClass().getSimpleName()).append('{');

			boolean first = true;
			for (int i = 0, listSize = list.size(); i < listSize; i++) {
				final PropertyDescriptor descriptor = list.get(i);
				if ("class".equals(descriptor.getName())) {
					continue;
				}
				final Object value = descriptor.getReadMethod().invoke(o);
				if (!showNull && (value == null || (value instanceof Collection && ((Collection) value).isEmpty()))) {
					continue;
				}
				if (first) {
					first = false;
				}
				else {
					s.append(", ");
				}
				s.append(descriptor.getName()).append("=").append(toString(value, showNull, processed));
			}
			s.append('}');

			return s.toString();
		}
		catch (IntrospectionException e) {
			throw new RuntimeException(e);
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private static String toString(Collection<?> coll, boolean showNull, Set<Object> processed) {
		if (coll == null) {
			return "null";
		}

		final StringBuilder s = new StringBuilder();
		s.append('[');
		boolean first = true;
		for (Object o : coll) {
			if (first) {
				first = false;
			}
			else {
				s.append(", ");
			}
			s.append(toString(o, showNull, processed));
		}
		s.append(']');

		return s.toString();
	}
}
