package ch.vd.uniregctb.utils;

import org.apache.commons.configuration.PropertiesConfiguration;

public class MockUniregProperties extends UniregProperties {

	private final PropertiesConfiguration props = new PropertiesConfiguration();

	public MockUniregProperties() {
		setProperties(props);
	}

	private void addProperty(String key, String value) {
		props.setProperty(key, value);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		addProperty("extprop.url.taopp", "https://blabla/_NOCTB_/_OID_");
		addProperty("extprop.url.taoba", "https://blabla/_NOCTB_/_OID_");
		addProperty("extprop.url.taois", "https://blabla/_NOCTB_/_OID_");
		addProperty("extprop.url.sipf", "https://blabla/_NOCTB_/_OID_");
		addProperty("extprop.ifosec.debug", "true");
	}
}
