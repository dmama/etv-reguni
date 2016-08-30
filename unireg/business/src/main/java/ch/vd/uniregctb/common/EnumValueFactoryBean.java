package ch.vd.uniregctb.common;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class EnumValueFactoryBean<E extends Enum<E>> implements FactoryBean<E>, InitializingBean {

	private final Class<E> enumClass;
	private String valueString;
	private E value;

	public EnumValueFactoryBean(Class<E> enumClass) {
		this.enumClass = enumClass;
	}

	public void setValueString(String valueString) {
		this.valueString = valueString;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		value = StringUtils.isBlank(valueString) ? null : Enum.valueOf(enumClass, valueString);
	}

	@Override
	public E getObject() throws Exception {
		return value;
	}

	@Override
	public Class<?> getObjectType() {
		return enumClass;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
