package ch.vd.uniregctb.hibernate.meta;

import org.hibernate.usertype.UserType;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.hibernate.meta.PropertyType;

public abstract class UserTypePropertyType extends PropertyType {
	private final UserType userType;

	public UserTypePropertyType(Class<?> javaType, UserType userType) {
		super(javaType, extractSqlType(userType));
		this.userType = userType;
	}

	private static int extractSqlType(UserType userType) {
		Assert.isTrue(userType.sqlTypes().length == 1, "Mapping d'un user-type sur >1 colonnes non supporté");
		return userType.sqlTypes()[0];
	}

	public UserType getUserType() {
		return userType;
	}

	public abstract String getConvertMethod(String value);
}
