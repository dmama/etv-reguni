package ch.vd.uniregctb.hibernate.meta;

import ch.vd.uniregctb.hibernate.EnumUserType;

public class EnumUserTypePropertyType extends UserTypePropertyType {
	public EnumUserTypePropertyType(Class<?> javaType, EnumUserType enumUserType) {
		super(javaType, enumUserType);
	}

	@Override
	public String getConvertMethod(String value) {
		return "Enum.valueOf(" + javaType.getSimpleName() + ".class, " + value + ")";
	}

	@Override
	public boolean needNullCheck() {
		return true; // nécessaire parce que le cast string -> enum ne supporte pas les strings nulles.
	}
}
