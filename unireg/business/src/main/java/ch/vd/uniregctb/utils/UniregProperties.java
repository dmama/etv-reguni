package ch.vd.uniregctb.utils;

import java.util.Map;

import ch.vd.registre.jmx.properties.PropertiesAdapterFactoryBean;
import ch.vd.registre.jmx.properties.UnsupportedPropertiesAdapter;

public interface UniregProperties {

	/**
	 * @return la map des propriétés externes et de leurs valeurs
	 */
	Map<String, String> getAllProperties();

	/**
	 * @param key le nom de la propriété
	 * @return la valeur associée à cette propriété
	 */
	String getProperty(String key);

	/**
	 * Property adapter factory bean
	 */
	static class UniregPropertiesAdapterFactoryBean extends PropertiesAdapterFactoryBean {
		@Override
		public UnsupportedPropertiesAdapter createInstance(Object obj) {
			if (obj instanceof UniregProperties) {
				obj = ((UniregProperties)obj).getAllProperties();
			}
			return super.createInstance(obj);
		}
	}

}
