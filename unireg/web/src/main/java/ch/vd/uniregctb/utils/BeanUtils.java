package ch.vd.uniregctb.utils;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;

public abstract class BeanUtils {

	private static final Logger LOGGER = Logger.getLogger(BeanUtils.class);
	private static final String [] EMPTY_STRING_ARRAY = new String[0];

	public static void simpleMerge( Object dest, Object src)  {
		Assert.notNull(src);
		Assert.notNull(dest);
		try {
			if ( !src.getClass().isAssignableFrom(dest.getClass())) {
				throw new RuntimeException("src != dest");
			}
			PropertyDescriptor[]  propertiesSrc = PropertyUtils.getPropertyDescriptors(src);
			for (PropertyDescriptor desc : propertiesSrc) {
				if (desc.getReadMethod() == null || desc.getWriteMethod() == null)
					continue;
				String name = desc.getName();
				Object srcValue = PropertyUtils.getProperty(src, name);
				if ( srcValue instanceof Collection<?> || srcValue instanceof Map<?, ?>){
					continue;
				}
				Class<?> type = desc.getPropertyType();
				String canonicalClass = type.getName();
				if ( type.isEnum() || type.isArray() || type.isPrimitive() || canonicalClass.startsWith("java.lang")) {
					PropertyUtils.setProperty(dest, name, srcValue);
				}
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
}

	/**
	 * Teste la présence d'un propriété null dans un bean
	 *
	 * @param bean le bean à tester
	 * @param ignoreProterties Liste des propriétés à ignorer lors de la verification
	 * @return true si une propriété est null
	 */
	public static boolean hasNullProperties(Object bean, String... ignoreProterties ) {
		PropertyDescriptor[]  propertyDescriptors = PropertyUtils.getPropertyDescriptors(bean);
		for (PropertyDescriptor pd : propertyDescriptors) {
			assert pd != null;
			assert pd.getReadMethod() != null;
			if (Arrays.asList(ignoreProterties).contains(pd.getName())) {
				continue;
			}
			try {
				if (PropertyUtils.getProperty(bean, pd.getName()) == null) {
					return true;
				}
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return false;
	}

	public static boolean isAssignable (Object beanSrc, Object beanDest, String... ignoreProperties) {
		return ListUtils.subtract(
				Arrays.asList(findMissingProperties(beanSrc, beanDest)),
				Arrays.asList(ignoreProperties)
		).isEmpty();
	}

	/**
	 * @see #areBeansAssignableToEachOther(Object, String[], Object, String[])
	 * @param bean1
	 * @param bean2
	 * @return
	 */
	public static boolean areBeansAssignableToEachOther (Object bean1, Object bean2) {
		return areBeansAssignableToEachOther(bean1, EMPTY_STRING_ARRAY, bean2, EMPTY_STRING_ARRAY);
	}

	/**
	 * @see #areBeansAssignableToEachOther(Object, String[], Object, String[])
	 * @param bean1
	 * @param bean1PropertiesToIgnore
	 * @param bean2
	 * @return
	 */
	public static boolean areBeansAssignableToEachOther (Object bean1, String [] bean1PropertiesToIgnore, Object bean2) {
		return areBeansAssignableToEachOther(bean1, bean1PropertiesToIgnore, bean2, EMPTY_STRING_ARRAY);
	}

	/**
	 * @see #areBeansAssignableToEachOther(Object, String[], Object, String[])
	 * @param bean1
	 * @param bean2
	 * @param bean2PropertiesToIgnore
	 * @return
	 */
	public static boolean areBeansAssignableToEachOther (Object bean1, Object bean2, String [] bean2PropertiesToIgnore) {
		return areBeansAssignableToEachOther(bean1, EMPTY_STRING_ARRAY, bean2, bean2PropertiesToIgnore);
	}

	/**
	 * TODO (fnr) commenter cette méthode
	 *
	 * @param bean1
	 * @param bean1PropertiesToIgnore
	 * @param bean2
	 * @param bean2PropertiesToIgnore
	 * @return
	 */
	public static boolean areBeansAssignableToEachOther (Object bean1, String [] bean1PropertiesToIgnore, Object bean2, String [] bean2PropertiesToIgnore) {
		return
		isAssignable(bean2, bean1, bean1PropertiesToIgnore) &&
		isAssignable(bean1, bean2, bean2PropertiesToIgnore);
	}

	/**
	 * TODO (fnr) commenter cette méthode
	 *
	 * @param beanToCheck
	 * @param beanToCompareFrom
	 * @return
	 * @throws IllegalArgumentException si un ou les 2 paramètres sont null.
	 */
	public static String[] findMissingProperties (Object beanToCheck, Object  beanToCompareFrom) {

		if (beanToCheck == null) {
			throw new IllegalArgumentException("beanToCheck ne peut pas être null");
		}
		if (beanToCheck == null) {
			throw new IllegalArgumentException("beanToCompareFrom ne peut pas être null");
		}

		PropertyDescriptor[]  propertyDescriptors = PropertyUtils.getPropertyDescriptors(beanToCompareFrom);
		ArrayList<String> listRes = new ArrayList<String>();
		for (PropertyDescriptor pd : propertyDescriptors) {
			try {
				if ( PropertyUtils.getPropertyDescriptor(beanToCheck, pd.getName()) == null) {
					listRes.add(pd.getName());
				}
			}
			catch (Exception e) {
				LOGGER.warn(e.getMessage(), e);
				listRes.add(pd.getName());
			}
		}
		return listRes.toArray(new String[listRes.size()]);

	}
}
