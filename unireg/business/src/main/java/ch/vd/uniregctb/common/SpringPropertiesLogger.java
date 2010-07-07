package ch.vd.uniregctb.common;

import org.apache.log4j.Logger;

public class SpringPropertiesLogger {

	public static final Logger LOGGER = Logger.getLogger(SpringPropertiesLogger.class);

	private final String title;

	public SpringPropertiesLogger(String title) {

		this.title = title;
	}

	public void setProperties(String properties) {

		// Change les | en \n (EOL)
		properties = properties.replace('|', '\n');

		String str = "\n";
		str += "=== Begin Dump of Spring properties ===";
		str += "=== "+title+" ===";
		str += properties;
		str += "\n=== End Dump of Spring properties ===";
		str += "\n";

		LOGGER.info(str);

	}
}
