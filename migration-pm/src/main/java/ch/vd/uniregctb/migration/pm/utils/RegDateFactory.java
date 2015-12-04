package ch.vd.uniregctb.migration.pm.utils;

import java.text.ParseException;

import org.springframework.beans.factory.FactoryBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

/**
 * Factory qui est capable de fournir une instance de {@link RegDate} à partir d'une chaîne de caractères
 */
public final class RegDateFactory implements FactoryBean<RegDate> {

	private final RegDate value;

	public RegDateFactory(RegDateHelper.StringFormat format, String value) throws ParseException {
		this.value = format.fromString(value, false);
	}

	@Override
	public RegDate getObject() throws Exception {
		return this.value;
	}

	@Override
	public Class<?> getObjectType() {
		return RegDate.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
