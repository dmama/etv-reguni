package ch.vd.uniregctb.common;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

import ch.vd.registre.base.utils.ObjectGetterHelper;

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
				s.append(descriptor.getName()).append('=').append(toString(value, showNull, processed));
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

	/**
	 * Retourne la valeur de l'attribut spécifiée par son chemin (en notation pointée).
	 *
	 * @param object un objet
	 * @param path   le chemin vers l'attribut dont on veut récupérer la valeur
	 * @return la valeur de l'attribut pointé.
	 * @throws java.beans.IntrospectionException    en cas de problème
	 * @throws IllegalAccessException    en cas de problème
	 * @throws java.lang.reflect.InvocationTargetException en cas de problème
	 */
	public static Object getPathValue(Object object, String path) throws IntrospectionException, IllegalAccessException, InvocationTargetException {
		if (path.contains(".")) {
			Object o = object;
			final String[] sub = path.split("\\.");
			for (String p : sub) {
				o = ObjectGetterHelper.getValue(o, p);
				if (o == null) {
					break;
				}
			}
			return o;
		}
		else {
			return ObjectGetterHelper.getValue(object, path);
		}
	}

	public enum SetPathBehavior {
		CREATE_ON_THE_FLY,
		PATH_MUST_EXISTS,
		FAILS_SILENTLY
	}

	public static void setPathValue(Object object, String path, Object value, SetPathBehavior behavior) throws IntrospectionException, IllegalAccessException, InvocationTargetException,
			InstantiationException {

		if (path.contains(".")) {
			// première phase, on va jusqu'au dernier objet
			Object o = object;
			final String[] pathes = path.split("\\.");
			for (int i = 0, subPathes = pathes.length; i < subPathes - 1; i++) {
				final String p = pathes[i];

				// Get the value
				Object sub = ObjectGetterHelper.getValue(o, p);
				if (sub == null) {
					// traitement particulier si les objets n'existent pas tout-au-long du chemin
					switch (behavior) {
					case FAILS_SILENTLY:
						return;
					case PATH_MUST_EXISTS:
						throw new IllegalArgumentException("One or more objects are null along path [" + path + "]");
					case CREATE_ON_THE_FLY:
						final PropertyDescriptor descr = new PropertyDescriptor(p, o.getClass());
						final Method setter = descr.getWriteMethod();
						if (setter == null) {
							throw new NullPointerException("Setter for field [" + p + "] doesn't exists.");
						}
						final Method getter = descr.getReadMethod();
						sub = getter.getReturnType().newInstance();
						setter.invoke(o, sub);
						break;
					default:
						throw new IllegalArgumentException("Behavior [" + behavior + "] is unknown.");
					}
				}

				o = sub;
			}

			// seconde phase, on met-à-jour la valeur de l'attribut
			final String last = pathes[pathes.length - 1];
			setValue(o, last, value);
		}
		else {
			setValue(object, path, value);
		}
	}

	private static void setValue(Object object, String name, Object value) throws IntrospectionException, IllegalAccessException, InvocationTargetException {
		PropertyDescriptor descr = new PropertyDescriptor(name, object.getClass());
		Method setter = descr.getWriteMethod();
		setter.invoke(object, value);
	}
}
