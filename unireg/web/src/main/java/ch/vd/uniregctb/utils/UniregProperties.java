package ch.vd.uniregctb.utils;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

public class UniregProperties implements InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(UniregProperties.class);

	private static final long serialVersionUID = -1052755277538441358L;

	private static final Pattern PASSWORD_PATTERN = Pattern.compile("(password|psswd)", Pattern.CASE_INSENSITIVE);

	private String filename;

	private PropertiesConfiguration properties;

	public UniregProperties() {
	}

	public void setFilename(String filename) throws Exception {
		this.filename = filename;
	}

	@SuppressWarnings("unchecked")
	public Map<String, String> getAllProperties() {

		final Map<String, String> map = new HashMap<String, String>();

		Iterator<String> keys = properties.getKeys();
		while (keys.hasNext()) {
			final String key = keys.next();
			final String value = properties.getString(key);
			map.put(key, value);
		}

		return map;
	}

	public void afterPropertiesSet() throws Exception {
		LOGGER.info("Unireg Properties filename: " + filename);

		if (filename.startsWith("${")) {
			throw new FileNotFoundException("The properties file '" + filename + "' is not valid");
		}

		properties = new PropertiesConfiguration();
		properties.setReloadingStrategy(new FileChangedReloadingStrategy());
		properties.setListDelimiter((char) 0); // Disabled
		// Fais le chargement a la fin quand on a disabled le delimiter
		properties.setFileName(filename);

		// Dump des properties
		String str = dumpProps(true);

		LOGGER.info("* Dump des properties depuis le fichier: '" + filename + "'\n" + str);
		LOGGER.info("*******");
	}

	public String dumpProps(boolean hidePasswords) {

		ArrayList<String> list = new ArrayList<String>();
		Iterator<?> keys = properties.getKeys();
		while (keys.hasNext()) {
			String k = (String) keys.next();
			list.add(k);
		}
		Collections.sort(list);

		String str = "";
		for (String key : list) {
			String value;
			if (hidePasswords && PASSWORD_PATTERN.matcher(key).find()) {
				value = "******";
			}
			else {
				value = properties.getString(key);
			}
			str += " * " + key + " => " + value + "\n";
		}
		return str;
	}

	public String getProperty(String key) {
		String value = properties.getString(key);
		return value;
	}

	/**
	 * Ce champ est protected pour permettre aux mock de setter la propriété
	 */
	protected void setProperties(PropertiesConfiguration p) {
		properties = p;
	}
}
