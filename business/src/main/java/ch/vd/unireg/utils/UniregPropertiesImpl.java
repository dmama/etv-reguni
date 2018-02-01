package ch.vd.unireg.utils;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public class UniregPropertiesImpl implements UniregProperties, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(UniregPropertiesImpl.class);

	private static final Pattern PASSWORD_PATTERN = Pattern.compile("(password|psswd)", Pattern.CASE_INSENSITIVE);

	private String filename;
	private String fileEncoding;

	private PropertiesConfiguration properties;

	public UniregPropertiesImpl() {
	}

	public void setFilename(String filename) throws Exception {
		this.filename = filename;
	}

	public void setFileEncoding(String fileEncoding) {
		this.fileEncoding = fileEncoding;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, String> getAllProperties() {

		final Map<String, String> map = new HashMap<>();

		Iterator<String> keys = properties.getKeys();
		while (keys.hasNext()) {
			final String key = keys.next();
			final String value = properties.getString(key);
			map.put(key, value);
		}

		return map;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		LOGGER.info("Unireg Properties filename: " + filename);

		if (filename.startsWith("${")) {
			throw new FileNotFoundException("The properties file '" + filename + "' is not valid");
		}

		properties = new PropertiesConfiguration();
		properties.setReloadingStrategy(new FileChangedReloadingStrategy());
		properties.setEncoding(fileEncoding);
		properties.setListDelimiter((char) 0); // Disabled
		// Fais le chargement a la fin quand on a disabled le delimiter
		properties.setFileName(filename);
		properties.load();

		// Dump des properties
		String str = dumpProps(true);

		LOGGER.info("* Dump des properties depuis le fichier: '" + filename + "'\n" + str);
		LOGGER.info("*******");
	}

	private String dumpProps(boolean hidePasswords) {

		ArrayList<String> list = new ArrayList<>();
		Iterator<?> keys = properties.getKeys();
		while (keys.hasNext()) {
			String k = (String) keys.next();
			list.add(k);
		}
		Collections.sort(list);

		final StringBuilder str = new StringBuilder();
		for (String key : list) {
			String value;
			if (hidePasswords && PASSWORD_PATTERN.matcher(key).find()) {
				value = "******";
			}
			else {
				value = properties.getString(key);
			}
			str.append(" * ").append(key).append(" => ").append(value).append('\n');
		}
		return str.toString();
	}

	public String getProperty(String key) {
		return properties.getString(key);
	}

	/**
	 * Ce champ est protected pour permettre aux mocks de setter la propriété
	 */
	protected void setProperties(PropertiesConfiguration p) {
		properties = p;
	}

}
