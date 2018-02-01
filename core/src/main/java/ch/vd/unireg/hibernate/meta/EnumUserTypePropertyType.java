package ch.vd.unireg.hibernate.meta;

import ch.vd.unireg.hibernate.EnumUserType;

public class EnumUserTypePropertyType extends UserTypePropertyType {
	public EnumUserTypePropertyType(EnumUserType enumUserType) {
		super(enumUserType, String.class);
	}
}
