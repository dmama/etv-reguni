package ch.vd.unireg.utils;

import org.apache.commons.configuration.PropertiesConfiguration;

public class MockUniregProperties extends UniregPropertiesImpl {

	private final PropertiesConfiguration props = new PropertiesConfiguration();

	public MockUniregProperties() {
		setProperties(props);
	}

	private void addProperty(String key, String value) {
		props.setProperty(key, value);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		addProperty("extprop.security.debug", "true");
		addProperty("iam.logout.url", "https://{HOST}/iam/accueil/");
	}
}
