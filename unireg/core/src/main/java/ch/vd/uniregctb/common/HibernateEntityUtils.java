package ch.vd.uniregctb.common;

import javax.persistence.Table;
import javax.persistence.Transient;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;

public abstract class HibernateEntityUtils {

	/**
	 * Cette méthode permet de forcer l'initialisation de toutes les collections (potentiellement) lazy-init d'une entité hibernate. Cette méthode descend récursivement sur toutes les entités des
	 * collections.
	 *
	 * @param entity une entité hibernate
	 * @throws Exception en cas d'erreur inattendue
	 */
	public static void forceInitializationOfCollections(HibernateEntity entity) throws Exception {

		final BeanInfo info = Introspector.getBeanInfo(entity.getClass());
		final PropertyDescriptor[] descriptors = info.getPropertyDescriptors();

		for (PropertyDescriptor desc : descriptors) {

			final Method getter = desc.getReadMethod();
			if (getter == null) {
				continue;
			}

			boolean isTransient = false;
			final Annotation[] annotations = getter.getAnnotations();
			for (Annotation a : annotations) {
				if (a instanceof Transient) {
					isTransient = true;
					break;
				}
			}
			if (isTransient) {
				continue;
			}

			final Object value = getter.invoke(entity);

			if (value instanceof Collection) {
				for (Object o : (Collection) value) { // force l'initialization de la collection
					if (o instanceof HibernateEntity) {
						forceInitializationOfCollections((HibernateEntity) o); // force l'initialisation des entités contenues dans la collection
					}
				}
			}
		}
	}

	/**
	 * Assert que les deux entités hibernate sont bien égales en descendant de manière récursive sur toutes les propriétés.
	 *
	 * @param expected l'entité de référence
	 * @param actual   l'entité à vérifier
	 * @throws Exception si les deux entités ne sont pas égales
	 */
	public static void assertEntityEquals(HibernateEntity expected, HibernateEntity actual) throws Exception {
		assertEntityEquals(expected, actual, "", new HashSet<EntityKey>());
	}

	@SuppressWarnings({"unchecked"})
	private static void assertEntityEquals(HibernateEntity expected, HibernateEntity actual, String path, Set<EntityKey> checkedSet) throws Exception {
		Assert.notNull(checkedSet);

		assertEqualsNullity(expected, actual, path);
		if (expected == null || actual == null) {
			return;
		}

		final Class<?> clazz = getEntityClass(expected);
		Assert.isEqual(clazz, getEntityClass(actual), "[" + path + "]");
		Assert.isEqual(expected.getKey(), actual.getKey(), "[" + path + "]");

		final EntityKey key = new EntityKey(expected);
		if (checkedSet.contains(key)) {
			// on a déjà vérifié les propriétés de cet object, on évite de partir en récursion
			return;
		}

		checkedSet.add(key);

		final BeanInfo info = Introspector.getBeanInfo(clazz);
		final PropertyDescriptor[] descriptors = info.getPropertyDescriptors();

		for (PropertyDescriptor desc : descriptors) {

			if (desc.getName().equals("class")) {
				continue;
			}

			if (desc.getName().equals("periodeFiscale") && clazz.equals(ModeleDocument.class)) {
				// FIXME (msi) [hack] le lien modeleDocument -> période fiscale n'est pas encore implémenté dans le dao Jdbc
				continue;
			}

			if (desc.getName().equals("modelesDocument") && clazz.equals(PeriodeFiscale.class)) {
				// FIXME (msi) [hack] le lien période fiscale -> modèles de documents n'est pas encore implémenté dans le dao Jdbc
				continue;
			}

			final Method getter = desc.getReadMethod();
			if (getter == null) {
				continue;
			}

			boolean isTransient = false;
			final Annotation[] annotations = getter.getAnnotations();
			for (Annotation a : annotations) {
				if (a instanceof Transient) {
					isTransient = true;
					break;
				}
			}
			if (isTransient) {
				continue;
			}

			final String propPath = path + "/" + desc.getName();

			final Object expectedValue = getter.invoke(expected);
			final Object actualValue = getter.invoke(actual);
			assertEqualsNullity(expectedValue, actualValue, propPath);
			if (expectedValue == null || actualValue == null) {
				continue;
			}

			if (expectedValue instanceof Collection) {
				if (expectedValue instanceof Set) {
					assertSetEquals((Set<HibernateEntity>) expectedValue, (Set<HibernateEntity>) actualValue, propPath, checkedSet);
				}
				else if (expectedValue instanceof List) {
					assertListEquals((List<HibernateEntity>) expectedValue, (List<HibernateEntity>) actualValue, propPath, checkedSet);
				}
				else {
					Assert.fail("[" + propPath + "] Only list and set are supported");
				}
			}
			if (expectedValue instanceof HibernateEntity) {
				assertEntityEquals((HibernateEntity) expectedValue, (HibernateEntity) actualValue, propPath, checkedSet);
			}
			else {
				if (expectedValue.getClass().isArray()) {
					Assert.fail("[" + propPath + "] Arrays are not supported");
				}
				Assert.isEqual(expectedValue, actualValue, "[" + propPath + "]");
			}
		}
	}

	private static Class<?> getEntityClass(HibernateEntity expected) {
		Class<?> clazz = expected.getClass();
		while (clazz.getName().contains("$")) { // hibernate crée des sous-classes des entités, et on ne s'intéresse qu'aux classes réelles
			clazz = clazz.getSuperclass();
		}
		return clazz;
	}

	private static void assertListEquals(List<HibernateEntity> expected, List<HibernateEntity> actual, String path, Set<EntityKey> checkedSet) throws Exception {
		Assert.isEqual(expected.size(), actual.size(), "[" + path + "]");
		for (int i = 0, expectedSize = expected.size(); i < expectedSize; i++) {
			final HibernateEntity e = expected.get(i);
			final HibernateEntity a = actual.get(i);
			assertEntityEquals(e, a, path + "/" + a.getKey(), checkedSet);
		}
	}

	@SuppressWarnings({"SuspiciousMethodCalls"})
	private static void assertSetEquals(Set<HibernateEntity> expected, Set<HibernateEntity> actual, String path, Set<EntityKey> checkedSet) throws Exception {
		Assert.isEqual(expected.size(), actual.size(), "[" + path + "]");

		final Map<Long, HibernateEntity> actualMap = new HashMap<Long, HibernateEntity>(actual.size());
		for (HibernateEntity a : actual) {
			actualMap.put((Long) a.getKey(), a);
		}

		for (HibernateEntity e : expected) {
			HibernateEntity a = actualMap.get(e.getKey());
			final String subPath = path + "/" + a.getKey();
			Assert.notNull(a, "[" + subPath + "]");
			assertEntityEquals(e, a, subPath, checkedSet);
		}
	}

	private static void assertEqualsNullity(Object expected, Object actual, String path) {
		if (expected == null && actual == null) {
			return;
		}
		if (expected == null || actual == null) {
			if (expected == null) {
				Assert.fail("[" + path + "] Expected is null but actual is " + actual);
			}
			else {
				Assert.fail("[" + path + "] Actual is null but expected is " + expected);
			}
		}
	}

	/**
	 * Détermine la classe de base (celle possédant l'annotation @Table) à partir d'une classe quelconque de la hiérarchie.
	 *
	 * @param clazz une classe faisant partie d'une hiérarchie de classes Hibernate
	 * @return la classe de base
	 */
	public static Class<?> getBaseClass(Class<?> clazz) {
		while (clazz != null) {
			final Annotation[] as = clazz.getAnnotations();
			if (as != null) {
				for (Annotation a : as) {
					if (a instanceof Table) {
						return clazz;
					}
				}
			}
			clazz = clazz.getSuperclass();
		}
		return null;
	}
}

