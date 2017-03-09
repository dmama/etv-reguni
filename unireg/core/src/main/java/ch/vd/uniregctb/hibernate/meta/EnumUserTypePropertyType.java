package ch.vd.uniregctb.hibernate.meta;

import ch.vd.uniregctb.hibernate.EnumUserType;

public class EnumUserTypePropertyType extends UserTypePropertyType {
	public EnumUserTypePropertyType(Class<?> javaType, EnumUserType enumUserType) {
		super(javaType, enumUserType);
	}
}
