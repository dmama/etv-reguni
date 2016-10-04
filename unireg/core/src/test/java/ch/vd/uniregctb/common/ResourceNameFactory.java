package ch.vd.uniregctb.common;

import java.util.UUID;

import org.springframework.beans.factory.FactoryBean;

public class ResourceNameFactory implements FactoryBean<String> {

	private final String prefix;

	public ResourceNameFactory(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public String getObject() throws Exception {
		return String.format("%s%s", prefix, UUID.randomUUID().toString());
	}

	@Override
	public Class<?> getObjectType() {
		return String.class;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}
}
