package ch.vd.uniregctb.dao.jdbc.meta;

import ch.vd.uniregctb.dao.jdbc.meta.UserTypeColumnType;
import ch.vd.uniregctb.hibernate.EnumUserType;

public class EnumUserTypeColumnType extends UserTypeColumnType {
	public EnumUserTypeColumnType(Class<?> javaType, EnumUserType enumUserType) {
		super(javaType, enumUserType);
	}

	public String getConvertMethod(String value) {
		return "Enum.valueOf(" + javaType.getSimpleName() + ".class, " + value + ")";
	}

	@Override
	public boolean needNullCheck() {
		return true; // nécessaire parce que le cast string -> enum ne supporte pas les strings nulles.
	}
}
