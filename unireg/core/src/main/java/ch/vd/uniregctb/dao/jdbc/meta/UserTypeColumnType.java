package ch.vd.uniregctb.dao.jdbc.meta;

import org.hibernate.usertype.UserType;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.dao.jdbc.meta.ColumnType;

public abstract class UserTypeColumnType extends ColumnType {
	private final UserType userType;

	public UserTypeColumnType(Class<?> javaType, UserType userType) {
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
