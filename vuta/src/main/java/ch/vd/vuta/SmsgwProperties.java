package ch.vd.vuta;

import java.net.URL;

public class SmsgwProperties {

	public static URL getDefaultLog4jConfigFile() {
		
		return SmsgwProperties.class.getResource("/log4j.xml");
	}
	
	public static String getLog4jConfigFile() {
		String str = getConfigFolder();
		if (str != null) {
			str += "/config/log4j.xml";
		}
		return str;
	}

	public static String getConfigFolder() {
		String configFolder = System.getProperty("ch.vd.appDir");
		return configFolder;
	}

	public static String getLoggingFolder() {
		String loggingFolder = getConfigFolder();
		if (loggingFolder != null) {
			loggingFolder += "/logs";
		}
		return loggingFolder;
	}

}
