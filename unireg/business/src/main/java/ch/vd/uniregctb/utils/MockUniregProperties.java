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
		addProperty("extprop.ifosec.debug", "true");
	}
}
